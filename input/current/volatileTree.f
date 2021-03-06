// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror

/*
	Alex: currently this is the most important test case, this file generates run-able
	output which terminates if executed and produces some output to stdout.

	<<<<<<< You should try NOT to break this. >>>>>>>>

*/

import ffi.stdio;
import ffi.stdlib;
import domains.*;

class BinTree
{
	BinTree& left;
	BinTree& right;
	int value;

	BinTree(BinTree left,int value,BinTree right)
	{
		this.left=left;
		this.value=value;
		this.right=right;
	}

	BinTree(int nvalue)
	{
		left=null;
		value=nvalue;
		right=null;
	}

	nonblocking int sum(int accum)
	{
		BinTree next;

		if(left!=null&&right!=null)
		{
			int ls=left.sum(0);
			int rs=right.sum(0);
			cancel ls+rs+accum+value;
		}
		else
		{
			if(left!=null)
				next=left;
			else if(right!=null)
				next=right;
			else
				cancel accum+value;
		}

		//could move cancel ls+rs+accum+value; into finally with ifexpr
		finally next.sum(accum+value);
	}

	nonblocking int sumseq(int accum)
	{
		BinTree next;

		if(left!=null&&right!=null)
		{
			cancel left.sumseq(0)+right.sumseq(0)+accum+value;
		}
		else
		{
			if(left!=null)
				next=left;
			else if(right!=null)
				next=right;
			else
				cancel accum+value;
		}

		//could move cancel ls+rs+accum+value; into finally with ifexpr
		finally next.sumseq(accum+value);
	}

	//non destructive add:
	BinTree add(int value)
	{
		if(value==this.value)
			cancel this;
		else if(value<this.value)
		{
			if(left!=null)
				cancel new BinTree(left.add(value),this.value,right);
			else
				cancel new BinTree(new BinTree(null,value,null),this.value,right);
		}
		else
		{
			if(right!=null)
				cancel new BinTree(left,this.value,right.add(value));
			else
				cancel new BinTree(left,this.value,new BinTree(null,value,null));
		}
	}
}

unique class uBinTree extends BinTree
{
	static uBinTree conv(BinTree bt)
	{
		if(bt==null)
			cancel null;
		else
		{
			uBinTree l=conv(bt.left);
			uBinTree r=conv(bt.right);
			cancel new uBinTree(l,bt.value,r);
		}
	}

	uBinTree(BinTree bt)
	{
		super(conv(bt.left),bt.value,conv(bt.right));
	}

	uBinTree(uBinTree nleft,int nvalue,uBinTree nright)
	{
		super(nleft,nvalue,nright);
	}

	uBinTree(int nvalue)
	{
		super(nvalue);
	}

	uBinTree add(int nvalue)
	{
		uBinTree left__next;
		uBinTree right__next;

		if(nvalue<value)
		{
			if(left!=null)
				left__next=((uBinTree)left).add(nvalue);
			else
				left__next=new uBinTree(null,nvalue,null);
			right__next=(uBinTree)right;
		}
		else if(nvalue>value)
		{
			left__next=(uBinTree)left;
			if(right!=null)
				right__next=((uBinTree)right).add(nvalue);
			else
				right__next=new uBinTree(null,nvalue,null);
		}
		else
		{
			left__next=(uBinTree)left;
			right__next=(uBinTree)right;
		}

		finally this where {left=left__next;right=right__next;};

	}

	uBinTree sum(int accum,out int val)
	{
		uBinTree left__next;
		uBinTree right__next;

//		int res;
//        uBinTree xx=sum(0,res);

		if(left!=null&&right!=null)
		{
			left__next=((uBinTree)left).sum(0,val__l);
			right__next=((uBinTree)right).sum(0,val__r);
			val = val__l+val__r+accum+value;
		}
		else
		{
			if(left!=null)
			{
				left__next=((uBinTree)left).sum(accum+value,val);
				right__next=(uBinTree)right;
			}
			else if(right!=null)
			{
				right__next=((uBinTree)right).sum(accum+value,val);
				left__next=(uBinTree)left;
			}
			else
			{
				val=accum+value;
				left__next=(uBinTree)left;
				right__next=(uBinTree)right;
			}
		}

		finally this where {left=left__next;right=right__next;};
	}

}

/*
class Test extends uBinTree
{}
*/

#define SIZE 100000

public class cur
{
	group gen;//we place gen_tree in group(gen) so that both funs are not executed concurrently
	//also we call srand() before each gen_tree, so the results should be identical

	static nonblocking group(gen) <T extends BinTree>
	T gen_tree(int n,T root)
	{
		if(n<=0)
			cancel root;

		finally gen_tree(n-1,root.add(stdlib.rand()));//SLOW!!
	}
/*
	static nonblocking group(gen) uBinTree gen_tree_U(int n,uBinTree root)
	{
		if(n<=0)
			cancel root;
		else
			finally gen_tree_U(n-1,root.add(stdlib.rand()));//SLOW!!
	}
*/
	static int f1(volatile BinTree b) //disallow return/out of transient vars?
	{
		int i;
		if(1<2)
		{
			b.sum(0);
			i=1;
		}
		else
		{
			i=0;
		}
		cancel i; //any fun exit must wait for all usages of b to finish before return! (add to tg)
	}

	static void f2(volatile BinTree b) //disallow return/out of transient vars?
	{
		//BinTree lb=b;
		cancel;
	}

	static int main(int argc,inout unique String[one_d{-1}] argv)
	{

		BinTree t;
		stdlib.srand(argc)::
		t=gen_tree(SIZE,new BinTree(0));
		#TIME(t,"gen_tree");//time (and dump) calc of t
		int res=t.sum(0);
		#TIME(res,"sum");

		int ress=t.sumseq(0);
		#TIME(ress,"sumseq");

		uBinTree t2;
		stdlib.srand(argc)::
		t2=gen_tree(SIZE,new uBinTree(0));
		#TIME(t2,"gen_tree_U");//time (and dump) calc of t

		volatile BinTree bt=t2; //transient only ref, cannot be passed into blocking/where update (basically no side effects)

		f1(bt);

		bt__2=bt;

		f1(bt__2);

		uBinTree next=(uBinTree)bt; //depends on anything that depends on bt (add to TG)

		BinTree upcast=next; //ok this way, downcast forbidden

		//uBinTree ups=upcast;

		finally 0;
	}
}