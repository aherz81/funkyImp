// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;
import stdlib.vector;
import stdlib.gVector;

#define TYPE int

#define FUNCNAME(HEIGHT, WIDTH) transpose_ ## HEIGHT ## _ ## WIDTH

#define BENCHMARK(HEIGHT, WIDTH) static TYPE[two_d{WIDTH, HEIGHT}] FUNCNAME(HEIGHT,WIDTH) (TYPE matrix[two_d{HEIGHT, WIDTH}])\
        {\
            cancel new TYPE[two_d{WIDTH, HEIGHT}].\[x,y]{matrix[y,x]};\
        }

#define MATRIX_NAME(HEIGHT, WIDTH) matrix_ ## HEIGHT ## _ ## WIDTH       
#define BENCHMARK_CALL(HEIGHT, WIDTH) TYPE MATRIX_NAME(HEIGHT, WIDTH)[two_d{HEIGHT, WIDTH}] = new TYPE[two_d{HEIGHT, WIDTH}].\[x,y]{(x*10)+(9-y)};\
                                      FUNCNAME(HEIGHT, WIDTH)(MATRIX_NAME(HEIGHT, WIDTH))
        
domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y } //two free
domain three_d{x,y,z}: one_d{x*y*z}(o) = {(j,k,l) | j<x & k < y & l < z} 

public class cur
{
        static TYPE[three_d{256,256,256}] f(TYPE ma[three_d{256,256,256}]) {
            cancel ma.\[a,b,c] {ma[c,a,b]};
        }
        
        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
            TYPE test[three_d{256,256,256}] = new TYPE[three_d{256,256,256}].\[x,y,z]{x+y+z};
            test'a = f(test);
            finally 0; //use finally and don't add extra wait?? does work with root??
        }
}

