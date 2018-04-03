// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;
import stdlib.vector;
import stdlib.gVector;

#define TYPE float

#define BLURNAME(HEIGHT, WIDTH) blur_ ## HEIGHT ## _ ## WIDTH

#define BLUR(HEIGHT, WIDTH) static TYPE BLURNAME(HEIGHT, WIDTH) (TYPE matrix[two_d{HEIGHT, WIDTH}], int x, int y) \
        { \
            if (x < 1 || y < 1 || x == HEIGHT - 1 || y == WIDTH -1) {\
                cancel 0.0f;\
            } else {\
                cancel matrix[x-1,y-1] + matrix[x-1,y] + matrix[x-1,y+1] + matrix[x,y-1] + matrix[x,y] + matrix[x,y+1] + matrix[x+1,y-1] + matrix[x+1,y] + matrix[x-1,y-1]; \
            } \
        }

#define FUNCNAME(HEIGHT, WIDTH) transpose_ ## HEIGHT ## _ ## WIDTH

#define BENCHMARK(HEIGHT, WIDTH) static TYPE[two_d{HEIGHT, WIDTH}] FUNCNAME(HEIGHT,WIDTH) (TYPE matrix[two_d{HEIGHT, WIDTH}])\
        {\
            cancel new TYPE[two_d{HEIGHT, WIDTH}].\[x,y]{BLURNAME(HEIGHT, WIDTH)(matrix, x,y)};\
        }

#define MATRIX_NAME(HEIGHT, WIDTH) matrix_ ## HEIGHT ## _ ## WIDTH       
#define BENCHMARK_CALL(HEIGHT, WIDTH) TYPE MATRIX_NAME(HEIGHT, WIDTH)[two_d{HEIGHT, WIDTH}] = new TYPE[two_d{HEIGHT, WIDTH}].\[x,y]{(x*10)+(9-y)};\
                                      FUNCNAME(HEIGHT, WIDTH)(MATRIX_NAME(HEIGHT, WIDTH))
        
domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y } //two free
domain three_d{x,y,z}: one_d{x*y*z}(o) = {(j,k,l) | j<x & k < y & l < z} 

public class cur
{
        static int g(int x) {
            cancel 2*x;
        }
        
        static int[three_d{256,256,256}] f(int ma[three_d{256,256,256}]) {
            cancel ma.\[a,b,c] {ma[c,a,b]+g(a)};
        }
        
        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
            int test[three_d{256,256,256}] = new int[three_d{256,256,256}].\[x,y,z]{x+y+z};
            test'a = f(test);
            finally 0; //use finally and don't add extra wait?? does work with root??
        }
}