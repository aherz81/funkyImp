// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;

#define MATRIX_SIZE 256

//#define OUTPUT

domain two_d{x,y} = { (j,k) | j<x & k<y } //two free

domain inner_2d{x,y} = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1}
//domain inner_2d{x,y}:one_d{(x-2)*(y-2)}(j) = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1 & j=a*x+b}

domain range{y,ys,ye}:one_d{ye-ys}(j) = { one_d{y}(a) | ys<=a & a<ye & ye<=y & j=a } //one free (a)

//alex: static dom params __{}__ are optional:
domain row{x,y}(j):one_d{y}(k) = { two_d{x,y}(a,b) | a=j & k=b } //one free(b)
domain col{x,y}(j):one_d{x}(k) = { two_d{x,y}(a,b) | b=j & k=a }	//one free(a)

//should be error, cause one_d{x} and trace{x,y} don__t have identical shape:
domain trace{x,y}:one_d{x}(j) = { two_d{x,y}(a,b) | j=a & a=b & x=y }// one free (a), indep of j //no order on items!! (warn??)

domain utriag{x,y} = { two_d{x,y}(a,b) | b>=a+1 & x=y }
domain ltriag{x,y} = { two_d{x,y}(a,b) | b<a & x=y }

//domain block{dx,dy,x,y}(i,j):two_d{dx,dy}(q,w) = { two_d{x,y}(a,b) | a>=i*dx & a<(i+1)*dx & b>=j*dy & a<(j+1)*dy & q=a & w=b }

//need templates to build nice matrix/vector classes

public class dom
{
	static int[two_d{4,4}] solve(int data[two_d{4,4}])
	{
		data.row(0).one_d.\[i] {data.row(0).one_d[i+1]+1};
		finally null;
	}

	static void test(int data[row{4,4}])
	{
		data.one_d.\[i] {data.one_d[i+1]+1};
	}
}

