// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;
import stdlib.vector;
import stdlib.gVector;

#define ARRAY_SIZE 917504
#define MATRIX_SIZE 32
#define BLOCK_SIZE 64

#define ARRAY_HEIGHT 64
#define ARRAY_WIDTH 32

//#define INNER_BLOCK
#define BLOCK
#define LINEAR
//#define DUMP

#define TYPE int

domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y } //two free

domain inner_2d{x,y} = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1}
//domain inner_2d{x,y}:one_d{(x-2)*(y-2)}(j) = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1 & j=a*x+b}

domain range{y,ys,ye}:one_d{ye-ys}(j) = { one_d{y}(a) | ys<=a & a<ye & ye<=y & j=a } //one free (a)

//alex: static dom params '{}' are optional:
domain row{x,y}(j):one_d{y}(k) = { two_d{x,y}(a,b) | a=j & k=b } //one free(b)
domain col{x,y}(j):one_d{x}(k) = { two_d{x,y}(a,b) | b=j & k=a }	//one free(a)

//should be error, cause one_d{x} and trace{x,y} don't have identical shape:
domain trace{x,y}:one_d{x}(j) = { two_d{x,y}(a,b) | j=a & a=b & x=y }// one free (a), indep of j //no order on items!! (warn??)

domain utriag{x,y} = { two_d{x,y}(a,b) | b>=a+1 & x=y }
domain ltriag{x,y} = { two_d{x,y}(a,b) | b<a & x=y }

public class cur
{
	static TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}] transposeRRR(TYPE ma[two_d{MATRIX_SIZE,MATRIX_SIZE}])
	{
            
            ma' = ma.\[r,c]{ma[c,r]}; 
            cancel ma';
	}

	static TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}] transpseRRR_mod(TYPE ma[two_d{MATRIX_SIZE,MATRIX_SIZE}])
	{
            cancel ma.\[r,c]{ma[c,r] * a(c)};
	}
        
        static TYPE a(TYPE i) 
        {
            cancel b((int)f(i)*3) + f(2);
        }
        
        static TYPE b(TYPE i) 
        {
            TYPE x = 5 * (7 + i);
            x'doubl = 2*x;
            cancel d(x'doubl);
        }
        
        static TYPE d(TYPE i) 
        {
            if (!(i < 0)) {
                cancel f(i*2) - f(5);
            } else if (i == 0) {
                cancel 9;
            } else {
                cancel g(i*3)*f(3);
            }
        }
        
        static TYPE f(TYPE i) 
        {
            cancel g(i*3);
        }
        
        static TYPE g(TYPE i) 
        {
            cancel 1;
        }
        
        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
                TYPE matrix[two_d{MATRIX_SIZE,MATRIX_SIZE}]=new TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { 10000000.f/(r*MATRIX_SIZE+c+1) };

		matrix'b = new TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { 10000000.f/(c*MATRIX_SIZE+r+1) };

		matrix'btrans=transposeRRR_mod(matrix'b);
		finally 0; //use finally and don't add extra wait?? does work with root??
        }
}


