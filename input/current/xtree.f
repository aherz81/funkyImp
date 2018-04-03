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
import ffi.math;
import domains.*;

#define SLOW 5000
#define SIZE 10000

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

        nonblocking BinTree addSlow(int value, int n)
        {
            if(n<0)
                cancel nop(value+n);
                
            finally addSlow(value,n-1);
        }
        
        BinTree nop(int value)
        {
            if(value==0)
                cancel add(value);
            else
                cancel this;
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
/*
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
        
        nonblocking uBinTree addSlow(int value, int n)
        {
            if(n<0)
                cancel nop(math.abs(value+n));
            
            finally addSlow(value,n-1);
        }
        
        uBinTree nop(int value)
        {
            if(value==-1)
                cancel add(value);
            else
                cancel this;
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

native class global //special: global will not be included
{
    static public long tick(long clocks); //busy wait for clocks cycles (uses rtdsc)
    static public int getWorkerThreadID();
    static public int getCurrentMaxThreadID();
    static public int rand();
    static public void srand(int seed);
}
/*
singular distributedCounter
{
    //only distributed with lock elision, otherwise would need too manually distribute (1 atomic? counter object per worker thread)
    int counter[one_d{-1}]; //may need some space in between vals so we don__t get an artificial collision

    distributedCounter()
    {
        counter = new int[one_d{math.max(8*16,global.getCurrentMaxThreadID()*16)}];
        //stdio.printf("Workers: %d\n",global.getCurrentMaxThreadID());
    }

    event increment()
    {
        //getWorkerThreadID ever > 1?
        int index=(global.getWorkerThreadID()*16)%counter.size[0];
        finally this where counter[index]=counter[index]+1;
    }

    int sampleValue()
    {
        int val=counter.\reduce{0}(i,accum){accum+counter[i]};
        cancel val;
    }
}
*/
singular xBinTree
{
    uBinTree ubt;
    //distributedCounter c;
    int finalItemCount;

    xBinTree(int val)
    {
        ubt=new uBinTree(val);
		//c=new distributedCounter();
		finalItemCount=-1;
    }

    event lastSubmitted(int targetItemCount)
    {
        /*
        if(c.sampleValue()>=targetItemCount)
        {
            //stdio.printf("insert finished\n");
            finished()=true;
        }
        */
        finally this where finalItemCount=targetItemCount;
    }

    event add(int val)
    {
        //c.increment();
        /*
        if(finalItemCount>0&&c.sampleValue()>=finalItemCount)
        {
                //stdio.printf("insert finished\n");
                finished()=true;
        }
        */
        
        //stdio.printf("Thread id %d\n",global.getWorkerThreadID());
                
        ubt__=ubt.addSlow(val,SLOW);//fixme finally this where ubt=ubt__.add(val); gives wrong error

        finally this where ubt=ubt__; //fixme this.ubt gives error!
    }

    final boolean finished();

    final int sum();

    event getSum(int val)
    {
        int res;
        ubt__=ubt.sum(val,res);
        sum()=res;
        finally this where ubt=ubt__;
    }

    int sampleItemCount()
    {
        //cancel c.sampleValue();
        cancel 0;
    }
}



public class ubintree
{
    group gen;//we place gen_tree in group(gen) so that both funs are not executed concurrently
    //group sum;
    //also we call srand() before each gen_tree, so the results should be identical

    static nonblocking group(gen) <T extends BinTree>
    T gen_tree(int n,T root)
    {
        if(n<=0)
            cancel root;

        finally gen_tree(n-1,root.addSlow(global.rand(),SLOW));//SLOW!!
    }

    static nonblocking group(gen)
    xBinTree gen_tree(int n,int max,xBinTree root)
    {
        if(n<=0)
        {
        
            //root.lastSubmitted(max);
            //int dummy=0;
            //#WORK(dummy,970000000)
            cancel root;
        }

        root.add(global.rand());//SLOW!!

        finally gen_tree(n-1,max,root);
    }

    static group(gen) int sumseq(BinTree bt)
    {
        cancel bt.sumseq(0);
    }

    static group(gen) int sumparBT(BinTree bt)
    {
        cancel bt.sum(0);
    }

    static group(gen) int sumparuBT(uBinTree ubt)
    {
        int res;
        ubt.sum(0,res);
        cancel res;
    }

    static int main(int argc,inout unique String[one_d{-1}] argv)
    {
/*    
        BinTree bt;
        global.srand(argc)::
        bt=gen_tree(SIZE,new BinTree(0));
        #TIME(bt,"_gen_tree");//time (and dump) calc of t
*/
/*                
        int res0;

        if(xt.finished())
        {
            //stdio.printf("received finished\n");
            res0=1;
        }
        else
            res0=0;
*/
        uBinTree ubt;
        global.srand(argc)::
        ubt=gen_tree(SIZE,new uBinTree(0));
        #TIME(ubt,"ugen_tree");//time (and dump) calc of t
        
        xBinTree xt;
        global.srand(argc)::
        xt=gen_tree(SIZE,SIZE,new xBinTree(0));
        #TIME(xt,"xgen_tree");//time (and dump) calc of t
        
/*
        int res1=sumparBT(bt);
        #TIME(res1,"sumparBT");

        int res2=sumseq(bt);
        #TIME(res2,"sumseqBT");

        int res3=sumparuBT(ubt);
        #TIME(res3,"sumparUBT");//time (and dump) calc of t         

        stdio.printf("sum=%d=%d=%d\n",res1,res2,res3);
*/
        finally 0;

        //finally res0;
    }
}
*/