// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;
import stdlib.vector;
import stdlib.gVector;

#define MATRIX_SIZE 4096
#define BLOCK_SIZE 64

#define INNER_BLOCK
#define BLOCK

//#define DUMP

#define TYPE float

domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y } //two free

//stupid defs cause pp doesn__t replace everything as it should do...
#define wrapper(n,a) n##a
#define wrapper2(n,a,b) n##a##b

//avoid non-linear constraints by substituting some constants early on:
#define region(dx,dy) wrapper2(region,dx,dy)
#define declregion(dx,dy) region(dx,dy){x,y}(i,j):two_d{dx,dy}(q,w) = { two_d{x,y}(a,b) | a>=i*dx & a<i*dx+dx & b>=j*dy & b<j*dy+dy & q=a & w=b }

domain declregion(BLOCK_SIZE,BLOCK_SIZE)

#define slicey(dy) wrapper(slicey,dy) //block row
#define declslicey(dy) slicey(dy){x,y}(i):two_d{dy,y}(q,w) = { two_d{x,y}(a,b) | a>=i*dy & a<i*dy+dy & q=a & w=b }

domain declslicey(BLOCK_SIZE)

#define slicex(dx) wrapper(slicex,dx) //block col
#define declslicex(dx) slicex(dx){x,y}(j):two_d{x,dx}(q,w) = { two_d{x,y}(a,b) | b>=j*dx & b<j*dx+dx & q=a & w=b }

domain declslicex(BLOCK_SIZE)


domain inner_2d{x,y} = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1}
//domain inner_2d{x,y}:one_d{(x-2)*(y-2)}(j) = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1 & j=a*x+b}

domain range{y,ys,ye}:one_d{ye-ys}(j) = { one_d{y}(a) | ys<=a & a<ye & ye<=y & j=a } //one free (a)

//alex: static dom params __{}__ are optional:
domain row{x,y}(j):one_d{y}(k) = { two_d{x,y}(a,b) | a=j & k=b } //one free(b)
domain col{x,y}(j):one_d{x}(k) = { two_d{x,y}(a,b) | b=j & k=a }	//one free(a)

//domain row{x,y}(j):one_d{y}(k) = { two_d{x,y}(a,b) | a=j & k=b } //one free(b)

//should be error, cause one_d{x} and trace{x,y} don__t have identical shape:
domain trace{x,y}:one_d{x}(j) = { two_d{x,y}(a,b) | j=a & a=b & x=y }// one free (a), indep of j //no order on items!! (warn??)

domain utriag{x,y} = { two_d{x,y}(a,b) | b>=a+1 & x=y }
domain ltriag{x,y} = { two_d{x,y}(a,b) | b<a & x=y }

public class cur
{

    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
		TYPE matrix[two_d{MATRIX_SIZE,MATRIX_SIZE}]=new TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { 10000000.f/(r*MATRIX_SIZE+c+1) };

//		TYPE x=matrix[1,2];
/*
		TYPE t=matrix.row(1).one_d[2];
		TYPE t2=matrix.col(1).one_d[2];


*/

		TYPE v=matrix.region(BLOCK_SIZE,BLOCK_SIZE)(2,3).two_d.row(4).one_d[5];

		TYPE r=matrix.slicey(BLOCK_SIZE)(1).two_d.slicex(BLOCK_SIZE)(2).two_d.row(3).one_d[4];

		finally 0; //use finally and don__t add extra wait?? does work with root??
    }
}

