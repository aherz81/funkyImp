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
        static TYPE[one_d{ARRAY_SIZE}] incrementAll(TYPE array[one_d{ARRAY_SIZE}])
        {
            array' = array.\[x]{array[x] + 1};
            cancel array';
        }

        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
                TYPE array[one_d{ARRAY_SIZE}] = new TYPE[one_d{ARRAY_SIZE}].\[x]{x};
                array'incremented = incrementAll(array);
		finally 0; //use finally and don't add extra wait?? does work with root??
        }
}


