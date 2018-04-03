#include "scheduler.h"
#include "../common/funkympi.h"
#include <iostream>

Scheduler::Scheduler()
{
}

Scheduler::~Scheduler()
{
}

/*
    initialization
*/
void Scheduler::init()
{
    myrank = MPI::COMM_WORLD.Get_rank();
    comsize = MPI::COMM_WORLD.Get_size();
    
    printf("SCHEDULER:\tScheduler rank: %d\n",myrank);
    printf("SCHEDULER:\tScheduler comsize: %d\n",comsize);
    
    //scheduler should always be 0, but just to make sure...
    if(myrank == 0)
    {
        scheduler_ready = true;
        MPI::COMM_WORLD.Bcast(&scheduler_ready, 1, MPI::BOOL, myrank);
        
        for(int i = 1; i < comsize; i++)
        {
            int rank;
            MPI::Status state;
            MPI::COMM_WORLD.Recv(&rank, 1, MPI::INT, MPI_ANY_SOURCE, REGISTER_TAG, state);
            
            if(state.Get_source() != 1)
            {
                //create process data structure
                process_t* proc = new process_t;
                proc->rank = rank;
                proc->communicator = MPI_COMM_WORLD;
                proc->state = IDLE;
                
                //add to list of all and free processes
                all_processes.insert(std::pair<int, process_t*>(rank, proc));
                idle_processes.push_back(proc);
            }
        }
        printf("SCHEDULER:\tClients registered\n");
        /*
        std::list<process_t*>::iterator iter = idle_processes.begin();
        while(iter != idle_processes.end())
        {
            process_t* p = *iter;
            std::cout<<"DEBUG: "<<p->rank<<std::endl;
            iter++;
        }
        */
    }
}

/*
    main scheduling loop
*/
void Scheduler::schedule()
{
    bool scheduling = true;
    MPI::Status state;
    int var;
    int sync = 1;
    while(scheduling)
    {   
        //Wait for request, which is a empty message
        //std::cout<<"SCHEDULER:\tWaiting for client message"<<std::endl;
        MPI::COMM_WORLD.Recv(&var, 1, MPI::INT, MPI_ANY_SOURCE, MPI_ANY_TAG, state);
        //std::cout<<"SCHEDULER:\t"<<"Received message from: "<<state.MPI_SOURCE<<std::endl;
        
        switch(state.Get_tag())
        {
            case REQUEST_TAG:
                handle_request(state.Get_source());
                break;
            case RELEASE_TAG:
                handle_release(state.Get_source());
                break;
            case EXIT:
                MPI::COMM_WORLD.Barrier();
                break;
            case SHUTDOWN:
            {
                std::cout<<"SCHEDULER: Stop scheduling..."<<std::endl;
                scheduling = false;
                break;
                }
            default:
                std::cout<<"SCHEDULER: TAG not known..."<<std::endl;
                break;
        }
    }
}

/*
    request handler
*/
void Scheduler::handle_request(int source)
{   
    //std::cout<<"idle processes..."<<idle_processes.size()<<std::endl;
    //std::cout<<"waiting processes..."<<waiting_processes.size()<<std::endl;
    if(idle_processes.size() == 0)
    {
        //std::cout<<"no idle process..."<<std::endl;
        process_t *p = all_processes[source];
        //no free worker processes available
        //enqueue requesting process
        std::cout<<"SCHEDULER: no free nodes"<<std::endl;
	p->state = WAITING;
        waiting_processes.push_back(p);
    }
    else
    {
        process_t *next = idle_processes.front();
        next->state = BUSY;
        busy_processes.push_back(next);
        
        //process_t *p = all_processes[source];
        
        //send rank of next process to requesting process
        int rank = next->rank;
        //std::cout<<"SCHEDULER:\t"<<"Answering request: "<<rank<<" to source "<<source<<std::endl;
        MPI::COMM_WORLD.Send(&rank, 1, MPI::INT, source, RESPONSE_TAG);
        
        idle_processes.pop_front();
    }
}

/*
    release handler
*/
void Scheduler::handle_release(int source)
{
    //std::cout<<"SCHEDULER:\t"<<"Idle process received: "<<source<<std::endl;
    process_t *p = all_processes[source];
    //find process in busy queue
    std::list<process_t*>::iterator it = busy_processes.begin();
    
    while(*it != p && it != busy_processes.end()) it++;
    
    if(p->rank == (*it)->rank)
    {
        busy_processes.erase(it);
        
        //process is now idle
        p->state = IDLE;
        idle_processes.push_back(p);
        //std::cout<<"SCHEDULER:\t"<<"Inserted new idle process: "<<source<<std::endl;
        
        //notify waiting processes that idle process has arrived
        notify();
    }
}

/*
    notification method for waiting processes
*/
void Scheduler::notify()
{
    if(!waiting_processes.empty() && !idle_processes.empty())
    {
        printf("SCHEDULER: notifying...\n");
        std::list<process_t*>::iterator waiter = waiting_processes.begin();
        std::list<process_t*>::iterator idler = idle_processes.begin();
        
        while(waiter != waiting_processes.end() && idler != idle_processes.end())
        {
            MPI::COMM_WORLD.Send(&((*idler)->rank), 1, MPI::INT, (*waiter)->rank, RESPONSE_TAG);
            waiting_processes.erase(waiter);
            idler++;
            waiter = waiting_processes.begin();
        }
    }
}
