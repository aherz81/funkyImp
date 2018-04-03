import ffi.stdio; //printf
import domains.linalg.*;
import domains.*;
import stdlib.*;

#define DIM 3 //probably should be 4 for simd: x,y,z,kind
#define TYPE double
#define NUM_ATOMS 4
#define STEPS 100
#define DT  ((TYPE)10.0)

//#define MatDIS TYPE[utriag{NUM_ATOMS, NUM_ATOMS}] //we have no wrapper for upper triag
#define MatFULL iMatrix<NUM_ATOMS, NUM_ATOMS,TYPE> //we have no wrapper for upper triag
#define rowD TYPE[row{NUM_ATOMS,DIM}] //shouldnt be required with proper auto casts
#define MatAxD iMatrix<NUM_ATOMS, DIM, TYPE>
#define VecD gVector<DIM,TYPE,rowD> 

//#define MatPxD iMatrix<NUM_ATOMS, 4, TYPE>

#define eps4_sig_7 ((TYPE)10.1)
#define eps4_sig_13 ((TYPE)10.1)

//use slicex to split before reduction!

domain takeRows{x,y}(j):two_d{j,y}(u,v) = { two_d{x,y}(a,b) | a<j & u=a & v=b } //one free(b)

public class fib
{   

    static unique MatAxD integrate(TYPE dt, MatAxD inData,MatAxD deltaot)
    {
        cancel inData.\row(i)
        {
            inData.rowVec(i)+deltaot.rowVec(i)*dt
        };
    }

    static inline TYPE force(TYPE oor2)
    {
        TYPE oor4=oor2*oor2; //compute required powers
        TYPE oor8=oor4*oor4;
        TYPE oor14=oor8*oor4*oor2;
        cancel (eps4_sig_7*oor8-eps4_sig_13*oor14); //4*eps*(sig^7/r^8-sig^13/r^14) will be mutiplied by r*direction
    }
    
    static MatFULL cacheForces(MatAxD pos)
    {    
        //compute all distances (actually, upper triag would be sufficient, what is the best mem layout for dists??)
        cancel new MatFULL().utriag().\[i,j] //only upper trace!
        {
            force(((TYPE)1)/(pos.rowVec(i) - pos.rowVec(j)).lengthSquare())
        }~null;                
    }
   
    static MatAxD calcForcesCached(MatAxD pos,MatFULL forces)
    {    
        //compute all forces
        cancel pos.\row(i) //for every atom
        {
            pos.row.\reduce{0}(j,accum) //must be inside pos.\row(i) so type of "0" is correctly deduced...(and res is generated into target)
            {
                if(j>i)
                {
                    (pos.rowVec(i)-pos.rowVec(j))*forces[i,j]+accum 
                }
                else
                {
                    (pos.rowVec(i)-pos.rowVec(j))*forces[j,i]+accum
                }
                //the vec substraction should be moved automatically transformed into a component wise substr to avoid the temporary vector
            }
        };                
    }           
        
    static MatAxD iterCached(TYPE dt,int steps,MatAxD pos,MatAxD vel)
    {
        if(steps<0)
        {
            cancel pos;
        }
        else
        {        
            MatFULL forces=cacheForces(pos);
            
            //VecD proj=pos.rowVec(0);
            
            pos__forces=calcForcesCached(pos,forces);
            vel__next=integrate(dt,vel,pos__forces); //update pos2 = pos1+dt*vel  
            pos__next=integrate(dt,pos,vel); //update pos2 = pos1+dt*vel            
            
            finally iterCached(dt,steps-1,pos__next,vel__next);
        }
    }
    

    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
        //prealloc buffers
        /*
        unique MatAxD pos1=new MatAxD(); //current pos
        unique MatAxD pos2=new MatAxD(); //next pos
        unique MatAxD vel1=new MatAxD(); //current speed
        unique MatAxD vel2=new MatAxD(); //next speed
        unique MatAxD forceList=new MatAxD(); //accumlated force per atom
        unique MatDIS force=new MatDIS; //forces between atoms
        */
        
        //iMatrix<1,1,int> = new iMatrix<1,1,int>().\[x,y] { 1 };
      
        //MatPxD atomProperties= new MatPxD();
        
        //read pos1 and vel1 from disk!!
        
        //pos1__result=iterBufferCached(DT,STEPS,pos1,pos2,vel1,vel2,forceList,force);
        
        
        MatAxD pos1=new MatAxD().\[i,j]{(TYPE)(1+i*NUM_ATOMS+j)};
        MatAxD vel1=new MatAxD();

        pos1.dumpRows("%f ","pos");
        
        pos1__result=iterCached(DT,STEPS,pos1,vel1);
                
        pos1__result.dumpRows("%f ","pos");
        
        //write pos1__result to disk
        
        finally 0; //use finally and don__t add extra wait?? does work with root??
    }
}
