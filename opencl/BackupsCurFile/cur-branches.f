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
        static TYPE noBranch(TYPE param) {
            TYPE calc = param * param - param;
            cancel calc * 2;
        }
        
        static TYPE oneBranch(TYPE param) {
            if (param % 2 < 1) {
                cancel noBranch(param);
            } else {
                cancel noBranch(param) + 1;
            }
        }
        
        static TYPE twoBranch(TYPE param) {
            if (param % 4 < 2) {
                cancel oneBranch(param);
            } else {
                cancel oneBranch(param) + 1;
            }
        }
        
        static TYPE threeBranch(TYPE param) {
            if (param % 8 < 4) {
                cancel twoBranch(param);
            } else {
                cancel twoBranch(param) + 1;
            }
        }
        
        static TYPE fourBranch(TYPE param) {
            if (param % 16 < 8) {
                cancel threeBranch(param);
            } else {
                cancel threeBranch(param) + 1;
            }
        }
      
        static TYPE fiveBranch(TYPE param) {
            if (param % 32 < 16) {
                cancel fourBranch(param);
            } else {
                cancel fourBranch(param) + 1;
            }
        }
        
        static TYPE sixBranch(TYPE param) {
            if (param % 64 < 32) {
                cancel fiveBranch(param);
            } else {
                cancel fiveBranch(param) + 1;
            }
        }
        
        static TYPE computeBranchless(TYPE param) {
            int a = param * param + 1;
            a'b = a + 1;
            a'c = a'b + 1;
            a'd = a'c + 1;
            cancel a'd + 1;
        }
        
        static TYPE computeWithBranches(TYPE param) {
            if (param % 16 == 0) {
                int a = param * param + 1;
                a'b = a + 1;
                a'c = a'b + 1;
                a'd = a'c + 1;
                cancel a'd + 1;
            } else if (param % 16 == 0) {
                int a = param * param + 1;
                a'b = a * 3;
                a'c = a'b + 1;
                a'd = a'c - 5;
                cancel a'd / 14;
            } else if (param % 16 == 1) {
                int a = param + param + 1;
                a'b = a * 3;
                a'c = a'b / 41;
                a'd = a'c - 14;
                cancel a'd + 4;            
            } else if (param % 16 == 2) {
                int a = param * param + 1;
                a'b = a + 1;
                a'c = a'b + 1;
                a'd = a'c + 1;
                cancel a'd + 1;
            } else if (param % 16 == 3) {
                int a = param - param + 1;
                a'b = a / 13;
                a'c = a'b - 1;
                a'd = a'c + 6541;
                cancel a'd * 3;
            } else if (param % 16 == 4) {
                int a = param + 1;
                a'b = a + 4;
                a'c = a'b - 1;
                a'd = a'c * 13;
                cancel a'd / 2;
            } else if (param % 16 == 5) {
                int a = param * param;
                a'b = a - 21;
                a'c = a'b + 11;
                a'd = a'c / 4;
                cancel a'd * 13;
            } else if (param % 16 == 6) {
                int a = param * param + 1;
                a'b = a * 31;
                a'c = a'b -31;
                a'd = a'c +21;
                cancel a'd / 3;
            } else if (param % 16 == 7) {
                int a = param / param + 1;
                a'b = a / 5;
                a'c = a'b - 1;
                a'd = a'c + 31;
                cancel a'd * 4;            
            } else if (param % 16 == 8) {
                int a = param * (2+param);
                a'b = a + 4;
                a'c = a'b / 6;
                a'd = a'c - 55;
                cancel a'd * 7;            
            } else if (param % 16 == 9) {
                int a = param * param + 1;
                a'b = a - 11;
                a'c = a'b / 12;
                a'd = a'c + 13;
                cancel a'd * 14;            
            } else if (param % 16 == 10) {
                int a = param * param + 1;
                a'b = a + 3;
                a'c = a'b - 4;
                a'd = a'c * 5;
                cancel a'd / 6;
            } else if (param % 16 == 11) {
                int a = param * param + 1;
                a'b = a / 3;
                a'c = a'b * 4;
                a'd = a'c - 5;
                cancel a'd + 6;            
            } else if (param % 16 == 12) {
                int a = param * param + 1;
                a'b = a + 3;
                a'c = a'b * 4;
                a'd = a'c - 5;
                cancel a'd / 6; 
            } else if (param % 16 == 13) {
                int a = param * param + 1;
                a'b = a - 3;
                a'c = a'b / 4;
                a'd = a'c + 5;
                cancel a'd * 6; 
            } else if (param % 16 == 14) {
                int a = param * param + 1;
                a'b = a * 3;
                a'c = a'b + 4;
                a'd = a'c - 5;
                cancel a'd / 6; 
            } else if (param % 16 == 15) {
                int a = param * param + 1;
                a'b = a + 36;
                a'c = a'b / 45;
                a'd = a'c * 54;
                cancel a'd - 67; 
            } else {
                int a = param * param + 1;
                a'b = a - 3;
                a'c = a'b * 4;
                a'd = a'c / 5;
                cancel a'd + 6; 
            
            }
        }
        
        static int main(int argc, inout unique String[one_d{-1}] argv)
        {          
                matrix'b.\[a,b] {fiveBranch(matrix'b[a,b])};
                matrix'b.\[a,b] {computeBranchless(matrix'b[a,b])};
                matrix'b.\[a,b] {computeWithBranches(matrix'b[a,b])};
		finally 0; //use finally and don't add extra wait?? does work with root??
        }
}


