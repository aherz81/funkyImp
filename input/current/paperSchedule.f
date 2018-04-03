// @PARAM: -XD-skipgenerate -verbose -regression -DEPGRAPH -d ./tmp
// @TEST noerror
//  ./custom.sh ./tmp/DepGraphPathTest.funky

//#define BRIDGE

import ffi.stdio;

public class cur
{
    static int experiment(int x)
    {
		x__1=g1(x);
		x__2=g1(x);

#WORK(x__1,5000)
#WORK(x__2,5000)

		cancel x__1+x__2;
    }

    static int fun(int x)
    {
		int a=g1(x);
		a__1=g1(x);
		int b=g1(a);
		b__a1=g1(a);

		b__b1=g1(a__1);
		b__b2=g1(a__1);

#WORK(b,0)
#WORK(b__a1,0)
#WORK(b__b1,0)
#WORK(b__b2,0)

		int d=b+b__a1+b__b1+b__b2;

		cancel d;
    }

    static int g1(int x)
    {
        int q=x;
        cancel q;
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