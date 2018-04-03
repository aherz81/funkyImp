#ifndef SCHEDULER_H
#define SCHEDULER_H

#include <mpi.h>
#include <list>

class Scheduler
{
    private:
        /*
            datatypes for processes
        */
        typedef enum
        {
            IDLE,
            BUSY,
            WAITING
        } p_state;

        typedef struct 
        {
            int rank;
            MPI_Comm communicator;
            p_state state;
            
        } process_t;
        
        /*
            member variables
        */
        bool    scheduler_ready;
        int     myrank;
        int     comsize;

        /*
            organizational lists for processes
        */
        std::map<int, process_t*>    all_processes;
        std::list<process_t*>        busy_processes;
        std::list<process_t*>        idle_processes;
        std::list<process_t*>        waiting_processes;
        /*
            private methods
        */
        void log(const char* string);
        void handle_request(int source);
        void handle_release(int source);
        void notify();
    
    public:
        /*
            public methods
        */
        Scheduler();
        ~Scheduler();
        void init();
        void schedule();    
};
#endif
