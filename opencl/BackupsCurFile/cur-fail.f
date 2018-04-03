// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;
import stdlib.vector;
import stdlib.gVector;

#define ARRAY_HEIGHT 3
#define ARRAY_WIDTH 4

#define TYPE int

domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y } //two free

public class cur
{
        static TYPE[two_d{ARRAY_WIDTH, ARRAY_HEIGHT}] transpose(TYPE matrix[two_d{ARRAY_HEIGHT, ARRAY_WIDTH}])
        {
            cancel new TYPE[two_d{ARRAY_WIDTH, ARRAY_HEIGHT}].\[x,y]{matrix[y,x]};
        }

        static int main(int argc, inout unique String[one_d{-1}] argv)
        {
            TYPE matrix[two_d{ARRAY_HEIGHT, ARRAY_WIDTH}] = new TYPE[two_d{ARRAY_HEIGHT, ARRAY_WIDTH}].\[x,y]{(x*10)+(9-y)};
            matrix.\[x,y]{stdio.printf("matrix(%d, %d) = %d\n", x, y, matrix[x,y])};
            stdio.printf("xxxxxxx matrix(0,3)=%d", matrix[0,3]);
            
            
            //stdio.printf("\n");
            //TYPE matrixT[two_d{ARRAY_WIDTH, ARRAY_HEIGHT}] = transpose(matrix);
            //matrixT.\[x,y]{stdio.printf("matrixT(%d, %d) = %d\n", x, y, matrixT[x,y])};
            
            finally 0; //use finally and don't add extra wait?? does work with root??
        }
}
