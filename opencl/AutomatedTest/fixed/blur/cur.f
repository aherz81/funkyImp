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
             cancel new TYPE[two_d{HEIGHT, WIDTH}].\[x,y]{BLURNAME(HEIGHT, WIDTH)(matrix,x,y)};\
         }

#define MATRIX_NAME(HEIGHT, WIDTH) matrix_ ## HEIGHT ## _ ## WIDTH       
#define BENCHMARK_CALL(HEIGHT, WIDTH) TYPE MATRIX_NAME(HEIGHT, WIDTH)[two_d{HEIGHT, WIDTH}] = new TYPE[two_d{HEIGHT, WIDTH}].\[x,y]{(x*10)+(9-y)};\
                                      FUNCNAME(HEIGHT, WIDTH)(MATRIX_NAME(HEIGHT, WIDTH))
        
domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y }

public class cur
{

            BLUR(32,32)
            BLUR(32,64)
            BLUR(64,64)
            BLUR(64,128)

            BLUR(128,128)
            BLUR(256,128)
            BLUR(256,256)
            BLUR(256,512)

            BLUR(512,512)
            BLUR(512,1024)
            BLUR(1024,1024)
            BLUR(2048,1024)

            BLUR(2048,2048)
            BLUR(4096,2048)
            BLUR(4096,4096)
            BLUR(4096,8192)

            BLUR(8192,8192)


            BENCHMARK(32,32)
            BENCHMARK(32,64)
            BENCHMARK(64,64)
            BENCHMARK(64,128)

            BENCHMARK(128,128)
            BENCHMARK(256,128)
            BENCHMARK(256,256)
            BENCHMARK(256,512)

            BENCHMARK(512,512)
            BENCHMARK(512,1024)
            BENCHMARK(1024,1024)
            BENCHMARK(2048,1024)

            BENCHMARK(2048,2048)
            BENCHMARK(4096,2048)
            BENCHMARK(4096,4096)
            BENCHMARK(4096,8192)

            BENCHMARK(8192,8192)

        
        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
            BENCHMARK_CALL(32,32);
            BENCHMARK_CALL(32,64);
            BENCHMARK_CALL(64,64);
            BENCHMARK_CALL(64,128);

            BENCHMARK_CALL(128,128);
            BENCHMARK_CALL(256,128);
            BENCHMARK_CALL(256,256);
            BENCHMARK_CALL(256,512);

            BENCHMARK_CALL(512,512);
            BENCHMARK_CALL(512,1024);
//            BENCHMARK_CALL(1024,1024);
//            BENCHMARK_CALL(2048,1024);

//            BENCHMARK_CALL(2048,2048);
//            BENCHMARK_CALL(4096,2048);
//            BENCHMARK_CALL(4096,4096);
//            BENCHMARK_CALL(4096,8192);

//            BENCHMARK_CALL(8192, 8192);

            
            finally 0; //use finally and don't add extra wait?? does work with root??
        }
}

