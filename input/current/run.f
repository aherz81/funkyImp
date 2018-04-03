// @PARAM: -XD-skipgenerate -verbose -regression -DEPGRAPH -d ./tmp
// @TEST noerror
//  ./custom.sh ./tmp/DepGraphPathTest.funky

//#define BRIDGE

import ffi.stdio;

public class cur
{

	static int longFun(int x)
	{
		if(x<0)
			cancel x;
		finally longFun(x-1);
	}

    static int fun(int x)
    {
		int a=g1(x);
		a__1=g1(x);
		int b=g1(a);
		b__1=g1(a);

		b__2=g1(a__1);
		b__3=g1(a__1);

		int d=g4(b,b__1,b__2,b__3);

		cancel d;
    }

    static int g1(int x)
    {
        int q=x;
        cancel q;
    }

    static int g2(int x,int y)
    {
		cancel x+y;
    }

    static int g3(int x,int y,int z)
    {
        cancel x+y+z;
    }

    static int g4(int x,int y,int z,int q)
    {
        cancel x+y+z+q;
    }

	static nonblocking int run(int iter,int val)
	{
		if(iter<=0)
			cancel val;

		finally run(iter-1,val+fun(val));
	}

    static int main(int argc,inout unique String[one_d{-1}] argv)
    {
		cancel run(100000,1000);
    }
}