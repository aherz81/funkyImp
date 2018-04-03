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

public class cur
{

        BENCHMARK(256,256)
        BENCHMARK(256,512)
        BENCHMARK(384,512)
        BENCHMARK(512,512)
        
        BENCHMARK(640,512)
        BENCHMARK(768,512)
        BENCHMARK(896,512)
        BENCHMARK(1024,512)
        
        BENCHMARK(768,768)
        BENCHMARK(640,1024)
        BENCHMARK(704,1024)
        BENCHMARK(768,1024)

        BENCHMARK(832,1024)
        BENCHMARK(896,1024)
        BENCHMARK(960,1024)
        BENCHMARK(1024,1024)

        BENCHMARK(1088, 1024)
        BENCHMARK(1152, 1024)
        BENCHMARK(1216, 1024)
        BENCHMARK(1280, 1024)
        
        BENCHMARK(1344, 1024)
        BENCHMARK(1408, 1024)
        BENCHMARK(1472, 1024)
        BENCHMARK(1536, 1024)
        
        BENCHMARK(1600, 1024)
        BENCHMARK(1664, 1024)
        BENCHMARK(1728, 1024)
        BENCHMARK(1792, 1024)

        BENCHMARK(1856, 1024)
        BENCHMARK(1920, 1024)
        BENCHMARK(1984, 1024)
        BENCHMARK(2048, 1024)
        
        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
            BENCHMARK_CALL(256,256);
            BENCHMARK_CALL(256,512);
            BENCHMARK_CALL(384,512);
            BENCHMARK_CALL(512,512);

            BENCHMARK_CALL(640,512);
            BENCHMARK_CALL(768,512);
            BENCHMARK_CALL(896,512);
            BENCHMARK_CALL(1024,512);

            BENCHMARK_CALL(768,768);
            BENCHMARK_CALL(640,1024);
            BENCHMARK_CALL(704,1024);
            BENCHMARK_CALL(768,1024);

            BENCHMARK_CALL(832,1024);
            BENCHMARK_CALL(896,1024);
            BENCHMARK_CALL(960,1024);
            BENCHMARK_CALL(1024, 1024);

            BENCHMARK_CALL(1088, 1024);
            BENCHMARK_CALL(1152, 1024);
            BENCHMARK_CALL(1216, 1024);
            BENCHMARK_CALL(1280, 1024);
            
            BENCHMARK_CALL(1344, 1024);
            BENCHMARK_CALL(1408, 1024);
            BENCHMARK_CALL(1472, 1024);
            BENCHMARK_CALL(1536, 1024);
            
            BENCHMARK_CALL(1600, 1024);
            BENCHMARK_CALL(1664, 1024);
            BENCHMARK_CALL(1728, 1024);
            BENCHMARK_CALL(1792, 1024);
            
            BENCHMARK_CALL(1856, 1024);
            BENCHMARK_CALL(1920, 1024);
            BENCHMARK_CALL(1984, 1024);
            BENCHMARK_CALL(2048, 1024);
            
            finally 0; //use finally and don't add extra wait?? does work with root??
        }
}
