import blah.x.y;

//basic types:

\N muint;
\Z msint;
\R mreal;
\S unicode_string;
\C unicode_char;
\I<\R> imag;
//\Q rational; //??
\B boolean;
\0 voidsih;
\U uniqueish;
\F<T> funcptr; //not first class!!!
\G mygroup;
\Bit<32> field; //bit field!! 

/*
bitfild ops(defined for \Bit<>):
shift left/right: <-,->
rotate left/right: <~,~>
and: &
or: |
xor: !|
not: !

boolean:

&&,||,!!
*/
/*
convert from/to bitfield?
*/

namespace MNS
{

enum Name //only == op on Name defined!
{
    a,b,c,d
}

enum TypeName:\N //enum TypeName is subtype of \N, ops of \N are inhertied
//(type derived from must have == operator!!)
{
    a(0),//constructor
    b(1),
    c(2),
    d(3)
}
    //names for numbers...

// Take care to make casts not ambigous with constructor, fun calls etc
fun \R tt(\N x,TypeName q)
{
	\R res=\cast<\R>(x);

	\N n = //can move assignment out of scope from case..
	case q//q has type TypeName -> don't need to qualify TypeName::a
	{
		| a -> /*n=*/0;
		| ? -> q;//q is derived from type \N
	}

	return res;
}

/*
enum TypeName:\N(index+1-1) //enum TypeName is subtype of \N, ops of \N are
//inhertied
{
    a,//constructor
    b,
    c,
    d
    //names for numbers...
}
*/
/*
interface iError =
    fun \S GetHRError()=0 //returns human readable version of the error


//add non fatal errors???
type Verify<T,E:iError,H:iSignal>:T@interface = //subtype constraint on e!,
//interface of T exposed:
    private:
        T &mValue=NIL;
        E &mError=NIL;
    public:
        Verify(T mv)
            mValue=mv

        Verify(E me)
            mError=me

        un_op T T@interface(Verify<T,E> v)
 //what to do if user uses interfce of T
            return (T)self
                        
        //cast to T:
        un_op T @(Verify<T,E> v)
            if(mError!=NIL)
                return mValue
            else
                fatal mError.GetHRError()
 //fatal takes string and exits immediately

        fun \B veFailed()
            return mError!=NIL
        fun \B veSuccess()
            return mError==NIL

        fun E veGetError()
            return mError
                */
//bit level operations (xor,or,and,...)??

//mc = Container<\Z>({i of \Z | i>0 & i<10})  //???
//\Z min = mc.extract_min()//heap impl //???

//irregular domains???

//Domain definition: domain name<param list>(arg list) = { (tuple of args) of Type | conditions on args }
//all domain indices are of \N -> don't sepcify
//domain one_d<y>(i) = { (i) | i < y }	//index set for an array, equivalent to [1..s]


domain one_d<y>(i) = { (i) | i < y }	//index set for an array, equivalent to [1..s]
domain two_d<x,y>(j,k) = { (j,k) | j<x & k<y } //index set for a 2d array
domain three_d<x,y,z>(j,k,l) = { (j,k,l) | j<x & k<y & l<z }

int ar[one_d<10>];

//subdomains
//SubDomain definition: domain name<param list>(arg list)[:generalization] = { Domain | conditions on args }

//domain calcs:

/*
- test constraints on constants (if any) -> error if fail
-	apply (independent) constraints until each index is finite (error if not) , allow fragmented (disjunct) intervals??
- dependent const with '=' -> dim --;
generate "for loop" with dependent constraint!
- dependent constraints must have simple form: a = formula , no minus!!!
- max one dep constraint per pair!
- min one unconstraint var!
fallback:
- if dependent constr -> generate set from intervals using constraints -> store set
- error if empty set

allow join and meet?
*/

domain range_1d<ys,ye>(j):one_d<ye-ys> = {one_d<y>(a) | ys<=a & a<ye}

domain row<x,y>(j):one_d<y> = { two_d<x,y>(a,b) | a=j }	//reduction of 2d array to a row (compatible with one_d)
domain col<x,y>(j):one_d<x> = { two_d<x,y>(a,b) | b=j }	//reduction of 2d array to col

domain area_x<x,y,z>(j,k):two_d<y,z> = { three_d<x,y,z>(a,b,c) | a=j }
domain area_y<x,y,z>(j,k):two_d<x,z> = { three_d<x,y,z>(a,b,c) | b=j }
domain area_z<x,y,z>(j,k):two_d<x,y> = { three_d<x,y,z>(a,b,c) | c=j }//pick a plane parallel to z

//diagonals:
domain udiag<x,y>(j):one_d<x-j> = { two_d<x,y>(a,b) | b=a+j & x=y } //a is unconst;'-' is forbidden in constraint LHS!
domain ldiag<x,y>(j):one_d<x-j> = { two_d<x,y>(a,b) | a=b+j & x=y } //b is unconst

domain utriag<x,y>(j,k) = { two_d<x,y>(a,b) | b>=a & x=y }
domain ltriag<x,y>(j,k) = { two_d<x,y>(a,b) | b<=a & x=y }

//reduction of 2d array to trace (works for all doms derived from two_d (like area))
domain trace<x,y>(j):one_d<x> = { two_d<x,y>(a,b) | a=b & x=y } 

//exclude borders
domain inner_2d<x,y>(j,k):two_d<x-2,y-2> = { two_d<x,y>(a,b) | a>0 & a<x-1 & b>0 & b < y-1 } 

domain border_2d_u<x,y>(j):one_d<x-2,1> = { two_d<x,y>(a,b) | a>0 & a<x-1 & b=0 } 
domain border_2d_d<x,y>(j):one_d<x-2,1> = { two_d<x,y>(a,b) | a>0 & a<x-1 & b=y } 
domain border_2d_l<x,y>(j):one_d<1,y-2> = { two_d<x,y>(a,b) | a=0 & b>0 & b < y-1 } 
domain border_2d_r<x,y>(j):one_d<1,y-2> = { two_d<x,y>(a,b) | a=x & b>0 & b < y-1 } 

//single is build in domain, all values are singles

domain edge_2d_ul<x,y>:single { two_d<x,y>(a,b) | a=0 & b=0 } 
domain edge_2d_ur<x,y>:single { two_d<x,y>(a,b) | a=x-1 & b=0 } 
domain edge_2d_dl<x,y>:single { two_d<x,y>(a,b) | a=0 & b=y-1 } 
domain edge_2d_dr<x,y>:single { two_d<x,y>(a,b) | a=x-1 & b=y-1 } 

//reinterpretation of domain
domain block<bx,by,x,y>(j,k):two_d<x/bx,y/by> = { two_d<x,y>(a,b) | j*bx < a <(j+1)*bx & k*by < b <(k+1)*by & x%bx=0 & y%by=0  } //reinterpret 2d array as blocks

domain nth<n,x,y,cx,cy>(j,k):two_2<x/n,y/n> = {two_d<x,y>(a,b) | a%n=cx & b%n=cy}

//stealing????

//reinterpretation of (sub-)domains

//FIXME: template constraints/PP/reflections?


type DynArray<T>
{
    private:
        
        T[one_d<?>] mData; //dynamic domain, has 2 additional domain options:
                          //resize(\n new_size), setdomain(\n dim)!
    public:
        DynArray()
				{
            mData.domain.resize(2);
				}

        dom domain(DynArray<T> m)
				{
            return m.mData;
				}
                
        fun \N items()
				{
            return mData.domain.domain(0);
				}

        fun DynArray<T> Add(t item) //add range??
				{
            if(mData.domain.domain(0)==mData.domain.size(0))
                mData'resize=mData.domain.resize(0,2*count);//mData.domain.resize(2*count,sizey) for 2d??
            else
                mData'resize=mData;
                    
            return self where 
						{
                mData=mData'resize where
								{
		              mData'resize.domain[mData.domain.domain(0)]=item;
		              mData'resize.domain.setdomain(mData'resize.domain.domain(0)+1);
								}
						}
				}

        fun DynArray<T> Remove(\N index)
				{
            return self where
						{
                mData.\range_1d(index,mData.domain.domain(0)-1)=mData.\range_1d(index+1,mData.domain.domain(0));
                mData.domain.setdomain(0,mData.domain.domain-1);
						}
				}

        //un_op t [1](\N index) //[1] == one dimensional
        //    return mData.domain[index]
}
                
//dynamic instead of type -> type contains type info??, all down casts are dynamic
//(@class_b)class_a //dyn cast??
type Matrix<x,y,t>	//define type Matrix with template args x,y,t
{
	//invariant:
            //must always be true..
    private:
				typedef tMarixData = t[two_d<x,y>];
				
        tMarixData mData;	//a set of type t that can by indexed over the domain two_d
            //\B test = false //

    public:
			Matrix(tMarixData data)
			{
					mData=data;
			}

      typedef ColVector<x,t> = Matrix<x,1,t>;	//def is just a (sub) typedef
      typedef RowVector<x,t> = Matrix<1,x,t>;	//row_vec

      //special operator for domains:
      dom domain(ColVector<x,t> m)//applying domain operations on ColVector is equivalent to apply them on mData
			{
          return m.mData.\col[0];
			}

      dom domain(RowVector<x,t> m)//applying domain operations on RowVector is equivalent to apply them on mData
			{
          return m.mData.\row[0];
			}

      dom domain(Matrix<x,y,t> m)//applying domain operations on Matrix is equivalent to apply them on mData
			{
          return m.mData;
			}

      fun Matrix<x,y,t> id() //create identity, no args, could be static, <x,y,t> is optional
			{
          return  //create object
              Matrix(Matrix<x,y,t>.domain(i,j) { i==j }) //only mdata is written (test get's default value = false)
                  ;
			}

      //ops are generic
      com_bin_op Matrix<a,b,c>  * (Matrix<a,b,c> m,<c> v) //binary operator takes a marix and a scalar
			{
          return Matrix(Matrix<a,b,c>.domain(i,j) { m.domain[i,j]*v }); //doesn't write test -> test is false
			}

      un_op Matrix<b,a,c>  ^T (Matrix<a,b,c> m) //unary op (applied via obj^op..allows to use letters as ops)
			{
					//tMatrixData md = Matrix<b,a,c>.domain(i,j){ m.domain[j,i] }
          return Matrix(Matrix<b,a,c>.domain(i,j){ m.domain[j,i] });
			}

      //match from top to bottom (vectors are matched before matrix)
      bin_op t * (RowVector<a,c> m1,ColVector<a,c> m2) //not commutative 
			{
          return \sum( m1.domain(j) , m1.domain[j]*m2.domain[j] ); //domain returns row[0] resp. col[0]
			}

		  bin_op Matrix<b,b,c> * (Matrix<a,b,c> m1,Matrix<b,a,c> m2) //not commutative
			{
		      return      
					Matrix(
              Matrix<b,b,c>.domain(i,j) { m1.\row[i]*m2.\col[j] }//object.domainomain[a,b,...]
          );                     
			}
		
			fun t lengthSq()
			{
		      return \sum( self.domain(i), self.D[i]*self.D[i] );
			}

			fun t trace()
			{ 
		      return \sum( self.\trace(j),self.\trace[j] );
			}
}

typedef Mat = Matrix<4,4,\R>; //define 4*4 matrix of reals


//
//argv.domain gives number of elements in D
typedef Handle = interface; //define Handle (unkown: hide impl)
//unsafe: //not thread save, no reentrance


//inout : the old object is destroyed and the new created at the instant of calling the func with inout ->
//two calls of the same function on the same object always give the same answer, 
//BUT: can never do a second call on an object accessed with inout as it doesn't exit anymore after the first call!!

unsafe Handle fopen(\S,\S) /*where fopen@tag=y;*/; 
unsafe \0 fwrite(inout Handle,\N,<T>); //inout means that the input is read and written to (only for extern funcs)
unsafe \N fread(inout Handle,\N,out <T>);//out means that the var is written only
unsafe \0 fseek(inout Handle,\N pos); //by default args are in
unsafe \0 fclose(inout Handle);

//all concurrent attempts to access a file will trigger a compiler error, as all members of File write to handle(but vals can be written only once!)
//therefore a compile error is triggered by stuff like:

/*
	File f
	f'1=f.Open()
	f'2=f'1.Read()
	f'3=f'1.Write()//error, this AND the prev line write the handle f'1.Func::handle'next: statik check or run time check necessary???
	
*/



type File /*where {File@tags=x;...}*/
{
//	File@group=FileIO //all members of this type are executed on the same thread
	//could set \G for fopen, fread, fwrite etc as well...
	/*
	tags:
		x=y;
	*/

	private:
		Handle& handle=Nil; //option ,unique (inferred via inout)

    File(Handle& h)
		{
        handle=h;
		}

	public:
		//make values used in destructive updates automatically linear??
		/*
		fun \0 TTT()
			handle'xxx=f(handle) //error, handle is linear
			fwrite([handle,handle'next],6,"xxx")//error:handle'xxx depends on handle, but handle is destroyed here

		fun Handle Get()
			return Handle //error, handle is linear because of other funcs

		*/

		static \B Open(\S name,\S mode,out File f)
		{
			return true where f=File(fopen(name,mode));//no err check...
		}

		fun File Write(<T> v)
		{
			fwrite([handle,handle'next],v@size,v@mem); //values marked with inout take two inputs in brakets: [in,out], handle is destroyed
			return File(handle'next);
		}

		//specialized
		fun File Write(\S s)
		{
			fwrite([handle,handle'next],\Z@size, \cast<\Z>(s@size) );
			fwrite([handle'next,handle'next2],s@size,s@mem);
			return File(handle'next2);
		}

		//handle mod
		//create val with size specified at creation time
		fun File Read(out <T> v)
		{
			fread([handle,handle'next],v@size,v@mem);
			return File(handle'next);
		}

		//specialized for strings
		fun File Read(out \S str)
		{
			\Z size;
			fread([handle,handle'next],size@size,size@mem);
			str@size=size; //writeable once
			fread([handle'next,handle'done],str@size,str@mem);
			return File(handle'done);
		}

		fun File Seek(\N pos)
		{
			fseek([handle,handle'next],pos);
			return File(handle'next);
		}

		final Close //must finish before app finishes
		{
			fclose(handle);
		}
}        
//\G FileIO

type MyMat = Mat where Mat@hint=mylab; //diff type/class??

/*export fun \B OnTimer()*/

//events: all funcs should be callable from any context!..how do they interact??

//dynamic arrays??
/*
fun \B(false) FindOption(DynArray<\S> argv,\S option) //plain function (returns false if result of body is undef)

	argv.domain(i) //do for all in domain of argv (op writes to res)
		if argv[i] == option
			return true //cancel all others
*/

fun \B FindOption(DynArray<\S> argv,\S option) /*where FindOption@group=FileIO*/ //plain function (returns false if result of body is undef)
{
		//map, reduce, etc?
    argv.domain(i)
		{
        if(argv.domain[i]==option)
            return true;
		}
//		if argv[i]==option
//			return false

    final false; //result if function is about to finish and no res has been produced
}

fun \Z Count(DynArray<\S> argv,\S option)
{
	return \reduce(+,0,argv.domain(i),\cast<\N>(argv.domain[i] == option));
}

MyMat ss = MyMat(Mat.domain(i,j) i-j);

//fun MyMat iterate(MyMat m,out \B finished)
fun MyMat iterate(MyMat m,out \B fail,\N c=0)
{
    m=m*ss;
		//can use select to execute statments:
		/*
			select 
			{
				| P -> call_fun();
			}
		*/
    return select //syntax sugar around complex if then else, tried top to bottom
		{
      | m.trace()<5.0 -> m where failed=false;
      | c>1000 -> m where failed=true;
      | ? -> iterate(mm,c+1);
		}
}

fun \Z main(DynArray<\S> argv) 
{
    \B option = FindOption(argv,"-transpose"); //option might become true if option is found, initially undef

    MyMat m = MyMat(Mat.domain(i,j) {i-j}); //need "Mat." ?

//		m'opt=option ? m^T:m;

		/*		
		//syntax sugar: when an lval is assigned some expression like this:
		m'opt=case option {| true -> m^T; SE(); | ? -> m;} //transform to: case option {| true -> m'opt=m^T; SE(); | ? -> m'opt=m;}
		m'opt=select {| option -> m^T; | ? -> m;}
    if(option) //reads option -> stalls until FindOption is finished if necessary
		{
				//has same scope as m!!!!!
        m'opt = m^T;	//m'identifer declares new state for matrix m
		}
    else
        m'opt = m; //MUST BE DEFINED FOR ALL CODE PATHS!
		
		//then the expressions type must be ident to the lval type, no additional side effects are allowed??
		//all eqivalent to following if:
		*/

		m'opt=if(option) m^T; else m;

		//m'opt=option?m^T:m;
		

    m't1=m^T; //reads/outputs completely ds
    m't2=m*m;

		m'tt=m where m[0,0]=0;
		m'tt2=m where m.\trace(i) m.\trace[i]=0;

		//apply filter:
		m'filtered= //union of disjoined domains
		(m.\inner_2d(i,j) {m[i-1,j-1]+m[i+1,j-1]+m[i-1,j+1]+m[i+1,j+1]+m[i,j])/5} ~
		(m.\border_2d_u(i) {m[i-1,j]+m[i+1,j]+m[i,j]+m[i,j+1])/4 } ~ 
		(m.\border_2d_d(i) {m[i-1,j]+m[i+1,j]+m[i,j]+m[i,j-1])/4 } ~
		(m.\border_2d_l(i) {m[i,j+1]+m[i,j+1]+m[i,j]+m[i+1,j])/4 } ~ 
		(m.\border_2d_r(i) {m[i,j-1]+m[i,j-1]+m[i,j]+m[i-1,j])/4 } ~
		(m.\edge_2d_ul() {m[i+1,j]+m[i,j+1]+m[i,j])/3 } ~
		(m.\edge_2d_ur() {m[i-1,j]+m[i,j+1]+m[i,j])/3 } ~
		(m.\edge_2d_dl() {m[i+1,j]+m[i,j-1]+m[i,j])/3 } ~
		(m.\edge_2d_dr() {m[i-1,j]+m[i,j-1]+m[i,j])/3 };

    \N r;
    //RowVector<m't2@TemplateArg@y,m't2@TemplateArg@t> rv = m't2.\\row(r)//compile time reflections

	
		//allow this:???
//    Matrix::RowVector<4,\R> rv = m't2.\\row(r);
    //rv is the first available (r-th) row in m't2
    /*may be available before the complete matrix is finished*/

		{ //block, will not be parallelized??
		  m'func1=func1(m); //can be executed anywhen after init of m

		  m'func2=func2(m'old);
		}


    //IO

    //can write code in sequential style:
    File f;

    \S out;

    if(File::Open("test.txt","r+b",f))
		{
        f.Write("test")
        .Seek(0)
        .Read(out)
        .Close();
		}
    else
        out="FAILED";

        //throw ???? anything that depends on File f

        //throw ???? anyth
    /*
            Handle h = mf(f.Get())
            f.Write("test")
            fwrite([h,h'new],...)//error:destroys h, also destroyed by f.Write

    */


    /*
    //or can be written in the painful long style:
    f'head=f.Write("test")

    f'reset=f'head.Seek(0)
    //f'lala=f'head.Write("lala") //error: both f'head.Seek and f'head.Write write f'head::handle (write only once)!

    \S out

    (f'r,out)=f'reset.Read()
    f'r.Close()
    */

    //m=m'3 //error! m can be written only once!

    m'final=func3(m'func1,m'func2);

    //scoped reuse of names IS VERY DANGEROUS!!, is it??

    \B fail;
    m'ff2=iterate(m'final,fail);
    //m'i=
/*
    do(i)
            m'i=
*/

    final m'ff2.\row[1].lengthSq(); //do NOT return here..it would preempt all other tasks!
}


interface stream //interface..but there can be only one!
{
	static stream& GetStream(); //MUST RETURN option if val should be deleteable at before exit
}

unsafe \0 print(inout stream s,\S format,<T> v);

type Output
{
	private:
		stream s = stream::GetStream(); //!!!???
	public:
    Outout(stream ns)
		{
        s=ns;
		}

		static Output print(\S format,\varargs,Output o)
		{
				return \varargs >>> o.print(format)
		}

    fun Output print(\S format,\varargs)
		{
        \varargs >>> print([s,s'done],format);//destroys s
        return self where s=(s'done);
		}
}

//keep list with ref to all instances??under which circumstances?
//data structures
type sBinTree<T> /*where {persistent=true;}*/
{
	private:

		t Val;
/*
		typedef Fut_BT = sBinTree where sBinTree@future=true;
		Fut_BT&	Left,Right; //always init to Nil?
*/
		sBinTree&	Left=Nil,Right=Nil; //always init to Nil?
	
	public:
    sBinTree(t v)
		{
        Val=v;
		}

    fun Output inorder(\B option=true,Output o)//Output o is destroyed when print is called!
		{
      if(self)//self!=Nil
			{
				o'res = (o)	>>> inorder(option,Left)
				 					>>> Output::print("%d",Val)
									>>> inorder(option,Right);
				/*
        o'left=inorder(option,Left,o);
        o'val=o'left.print("%d",Val);//calls print -> derive that o is destroyed in this func!!!
        o'right=inorder(option,Right,o'val);
				*/

        if(option)
          return o'res.print("\n");
        else
          return o'res;
			}
      else
        return o;
		}

    /*
    \0 inorder(inout Output o,sBinTree t=self)//ouput MUST be inout or destroy, as inorder calls Output.print which is inout
            if t
                    inorder([o,o'left],Left)
                    o'val=o'left.print("%d",Val)
                    inorder([o'val,o],Right)
                    return //o written in prev line
            else
                    return //in==out
    */
    /*
            f':
            type 'a bintree = Node of ('a bintree*'a*'a bintree) | Leaf

            let rec find v = function
                    | Node(l,val,r) ->	if val = v then true
                                                            else if v < val then find v l
                                                            else find v r
                    | ? -> false


            poor man's find:
            \B findOnly(t v,sBinTree n=self)
                    return select
                            | n.v==v -> true where s=n //where clause also allows to return multiple args!
                            | n.v>v ->	findOnly(v,n.Left)
                            | n.v<v ->  findOnly(v,n.Right)
                            | ? -> false //n=Nil if we come here...
    */

/*
obj where
    modifications or return args

*/

    //lambda expressions???
    //sBinTree<\Z>.map( \Z \L(\Z v) return v+1 )
    fun sBinTree map(\F<t \L(t)> f )
		{
        return self where
				{
            self.domain(o) //walks all nodes (potentially parallel)
                o=sBinTree(f(o.Val)) //reading from source (self), writing to target (copy of self)
				}
		}

    //out params can be discarded intentionally (starting from the right most)..just call \B f=sBinTree.find(5)

    //args with default arg are optional, out args are optional as well...so we can call: t max=o.findMax()
    fun t findMax(out sBinTree n)
		{
        return select
				{
            | s.Right -> s.Right.findMax(n);
            | ? -> Val where n=s;
				}
		}

    fun \B find(t v,out sBinTree* s,sBinTree n=self)
		{
        return select //need return statement!!! tailcal optimize select!
				{
            | n.Val==v -> true where s=n; //where clause also allows to return multiple args!
            | n.Val>v && n.Left -> n.Left.find(v,s);
            | n.Val<v && n.Right-> n.Right.find(v,s);
            | ? -> false where s=n;
				}
		}

    fun sBinTree insert(t v)
		{
        sBinTree* n;//reference that can be used for "writing" inside where clause
        if (find(v,n)) return self;
        else
				{
            if (v<n.Val)
                return self where n.Left=sBinTree(v); //incremental update (add stamped node to n, self has same stamp!)
            elsev
                return self where n.Right=sBinTree(v);
				}
		}

    fun sBinTree RemoveMax(out t max,sBinTree s=self,sBinTree start=self)
		{
        return select
				{
            | s.Right.Right -> RemoveMax(max,s.Right,start);
            | s.Right -> start where {
                    max=s.Right.Val//read old (could do this below "s.Right=Nil" as this does NOT destroy the old value!!)
                    s.Right=Nil //write new!!!!  GC!!
									 }	
            | ? -> Nil where max=s.Val;
				}
		}

    fun sBinTree remove(t v)
		{
        sBinTree* n;
        if (!find(v,n)) return self; //create find with parent?
        else
				{
          if (!n.Left) return self where n=n.Right; //destruct n??
              else if (!n.Right) return self where n=n.Left; //destruct n??
              else return self where;
                  n.Left=n.Left.RemoveMax(n.Val);
				}
		}
}


//quicksort: (in place update) ..incremental update

//iterative stuff: -> recursive..

//3d: sort by material!!! (sync) (locking!!) //partial results

//interfaces!!!: ...

//gui (events) ????




/*
	autogen petri net from source input -> do proving??
	
*/

fun \Z test()
{	
	Output o;
	sBinTree<\Z> sbt,sbt2;
	//o'done=sbt.inorder(o)

	//o't=o //allowed??->would be nice

	//special operator, pass as last argument(s), left associative!!
	o'2=o >>> sbt.inorder(false) >>> sbt2.inorder(); //write sbt2 after sbt
	//above is short for:
	//o'2=sbt2.inorder(sbt.inorder(o,false))


	//o'done2=sbt.inorder(o)//erroro was destroyed by o'done=sbt.inorder(o)

}

//return without killing the rest and without waiting for the rest: resume (rest must not return anything)


interface unique DrawPrim //interface..but there can be only one!
{
	\G GFX;//group
	DrawPrim@group=GFX; //should prob be in hints..do all gfx in one thread

	fun DrawPrim SetCol(\N col); //inherit \G from DrawPrim
	fun DrawPrim DrawRect(\Z x1,\Z y1,\Z x2,\Z y2);
	fun DrawPrim DrawText(\Z x1,\Z y1,\S txt);
	static DrawPrim GetDP();
}

singular GUIElement
{
	event eKill();
	//event eSetParent(GUIElement parent)
	event eAdd(GUIElement gelt);
	event eRemove(GUIElement gelt);

	fun \S GetName()=0; //must be impl
	fun DrawPrim Draw(DrawPrim dp)=0; //clip region??
	fun DrawPrim Update(DrawPrim dp);
}

singular Desktop
{
	event eAdd(GUIElement gelt); //receive events
	event eRemove(GUIElement gelt); //stop receiving elts
	//event eRequestDraw()
  event eUpdate(GUIElement gelt);
	static Desktop GetDesktop(); //desktop is unique but there can be many channels

	fun DrawPrim Process(DrawPrim dp);
}

singular Timer<T>
{
	typedef timer_fun = \F<\0 \L(\N time,<T> val)>;
public:
  static fun \N GetTime();
  Timer(\N dt,timer_fun f,<T> val);
}

//eh is singular (there can be no readable reference to any instance)
//events for one receiver are processed non concurrently!
singular GUIButton:GUIElement
{
/*
	semi disjoined state??
*/
	private: //local state:!! (hidden from the rest of the world)
		GUIElement& mParent=NIL;
		\Z count=0;
		\S txt="";
		\Z[one_d<4>] mCoords; //x1,y1,x2,y2
		const \S name="MyButton";

		fun MyButton SetCoords(\Z x1,\Z y1,\Z x2,\Z y2)
		{
			return self where 
			{
				mCoords[0]=x1;
				mCoords[1]=y1;
				mCoords[2]=x2;
				mCoords[3]=y2;
			}
		}

	public:
		GUIButton(GUIElement parent):GUIElement() //guaranteed first event (constructor)
		{
			parent->eAdd(self); //self is a channel...
			resume self where 
				mParent=parent;
		}
				
		//event destroy()
			//??

		//default impl??
		event eAdd(GUIElement gelt)
		{
      ASSERT(0,"Cannot add to Button");
      resume self;
		}

		event eRemove(GUIElement gelt)
		{
      ASSERT(0,"Cannot remove from Button");
      resume self;
		}
/*
		event eButtonPressed()			
			self'txt=SetText(\S(count+1))
			return self'txt where count'txt++			
*/		
		event eKill()
		{
      return NIL; //event must return self type, or NIL, event receive is unique!
		}

		event eSetText(\S ntxt)
		{
      return self where txt=ntxt;
		}

		event eSetCoords(\Z x1,\Z y1,\Z x2,\Z y2)
		{
      return SetCoords(x1,y1,x2,y2);
		}

		fun \S GetName() //safe event (does not read from self or write to self)
		{
      return name; //ok, name is const
		}

		fun DrawPrim Update(DrawPrim dp)
		{
      return dp;
		}

		fun DrawPrim Draw(DrawPrim dp) //reads from self, not concurrent
		{
        return dp
          .SetCol(0xaaaaaa)
          .SetTexture(NIL)
          .DrawRect(mCoords[0],mCoords[1],mCoords[2],mCoords[3])
          .DrawText(mCoords[0]+5,mCoords[1]+5,txt);
		}


		//error: events do NOT return data
		/*
		event Test(\R f,outret \R r)
			return self where r=f+mCoords[0]
		*/
}
//TIMER!!
//
			
singular MyButton:GUIButton
{
	private:
  	Desktop desktop;
		GFXTexture& tex=NIL;
		\S Texname="";
    \B reload=false;
		\N last_time=0;
		\N delta_time=0;
	public:
    MyButton(Desktop dd)
		{
      return self where desktop=dd;
		}

		event eButtonPressed()			
		{
      Desktop::GetDesktop()->eKill();
      \N cur_time=Timer<\0>::GetTime();
      delta_time=cur_time-last_time;
      eSetText(\S(delta_time));
      resume self where last_time=cur_time;
		}

		event eSetTexName(\S nn)
		{
        return self where
				{
          Texname=nn;
          desktop->eUpdate(self);  //request update with drawprim from desktop
				}
		}
                                      //(avoid select statement in Draw)
    event eSetTexture(GFXTexture nn)	
		{
        return self where tex=nn;
		}

    fun DrawPrim Update(DrawPrim dp)
		{
        GFXTexture ltex;
        return dp.LoadTexture(Texname,ltex)//update self.tex
            where eSetTexture(ltex); //fun MUST NOT write to self!
		}

		fun DrawPrim Draw(DrawPrim dp)
		{
        return dp
            .SetCol(0xaaaaaa)
            .SetTexture(tex)
            .DrawRect(mCoords[0],mCoords[1],mCoords[2],mCoords[3])
            .DrawText(mCoords[0]+5,mCoords[1]+5,txt);
		}

                //returns COPY of state!!
    fun \N GetState(\N state=0)//optional arg to induce order on getstate,
		{
        return delta_time;
		}
}	

singular GUIContainer:GUIElement
{
	private:
		GUIElement& mParent=NIL;
		DynArray<GUIElement> mChilds;

	public:
		
		GUIContainer(GUIElement parent) //guaranteed first event (constructor)
		{
			parent->eAdd(self);
			resume self where 
				mParent=parent;
		}
				
		//event destroy()//needed?
			//??

		event eAdd(GUIElement gelt)
		{
			//mParent->eAdd(mParent) //error, cannot send own state
			return self where 
				mContainer=mContainer.add(gelt);
		}

		event eRemove(GUIElement gelt)
		{
			return self where 
				mContainer=mContainer.remove(gelt);
		}

		event GetName(outret \S oname) //const fun! (does not change self)
		{
			return self where oname=\cast<\S>(self@type); //ok, name is const
		}

		fun DrawPrim Draw(DrawPrim dp) //const fun!
		{
//			mChilds.domain(i) dp'<i+1>=mChilds[i]->Draw(dp'<i>); //explicit dependency, generated name (stuff inside <> is calculated)
			mChilds.domain(i) dp=mChilds[i]->Draw(dp); //explicit dependency, generated name (stuff inside <> is calculated)

			//could do it recursively...as well..name clashes...!!

//			return dp'<mContainer.domain.domain(0)>;
			return dp;
		}
}
/*
type GraphNode<T>
{
  public:
    t mData;
    \N mId;
    DynArray<GraphNode<T>>> mFrom;
    DynArray<GraphNode<T>>> mTo;
    \B mDisabled=0;

    GraphNode(\N id)
		{
        mId=id;
		}

    GraphNode(\N id,t data,DynArray<GraphNode<T>>> from, DynArray<GraphNode<T>>> to)
		{
        mId=id;
        mData=data;
        mFrom=from;
        mTo=to;
		}
}

singular sTraverseState<t,c>
{
  private:
      Array<\B,c> mVisited;
      Array<\N,c> mOrder;
  public:
    sTraverseState(\N node_count)
        return self where
            mVisited=mVisited.domain.setsize(node_count).D(i)=0
            mOrder=mVisited.domain.setsize(node_count)

    final Array<\N,c> GetOrder(out \N count);
            //anything that depends on a final value becomes an exra code block!
            //args are forbidden!

    event eVisitNode(\N id)
		{
      if (mVisited.domain[id])
          return self;
      else
          return self where
					{
            mVisited.domain[id]=true;
            mOrder'next=mOrder.Add(id);
            mOrder=mOrder'next;
            if (mOrder'next.domain.domain(0)==mOrder'next.items()) //trigger!
                GetOrder(mOrder'next.domain.domain(0))=mOrder'next;
					}
		}
}
        
type Graph<T>
{
  private:
    DynArray<GraphNode<T>>> mNodeList = DynArray<GraphNode<T>>>(GraphNode<T>(0)); //container constructor?? FIXME
    LinkedList<\N> mFreeNodes = LinkedList<\N>(0);
  public:

    GraphNode<T> GetNode(\N id=0)
		{
        return mNodeList[id];
		}

    Graph AddNode(t data,out \N id, DynArray<GraphNode<T>>> from, DynArray<GraphNode<T>>> to)
		{
        return self where
				{
            \N id;
            if (mFreeNodes.domain.domain(0)==0)
                    id=mNodeList.domain;
            else
                    mFreeNodes=mFreeNodes.RemoveFirst(id);

            GraphNode<T> g=GraphNode<T>
            (
                    id, //should be based on partial order induced by from/to?
                    data,
                    from,
                    to
            );

            mNodeList=mNodeList.InsertAt(id,g);

            from.domain(i)
                    GetNode(from.domain[i]).mTo=GetNode(from.domain[i]).mTo.Append(id);

            to.domain(i)
                    GetNode(to.domain[i]).mFrom=GetNode(to.domain[i]).mFrom.Append(id);
            //update from/to info!!!
				}
		}


    Graph Remove(\N id)
		{
        return self where
				{
            mNodeList.domain[id].mDisabled=true;
            mFreeNodes=mFreeNodes.Append(id);

            //fix links
            mFrom.domain(i)
                GetNode(mFrom.domain[i]).mTo=GetNode(mFrom.domain[i]).mTo.Remove(id);

            mTo.domain(i)
                GetNode(mTo.domain[i]).mFrom=GetNode(mTo.domain[i]).mFrom.Remove(id);
				}
		}

    Graph Map(\F<GraphNode<T> \L(GraphNode<T>)> f)
		{
        return self where
				{
            mNodeList.domain(i)
            	mNodeList.domain[i]=f(mNodeList.domain[i]);
				}
		}

          //node removal??
}
*/
fun \0 timer_fun(\N time,MyButton button)
{
    button->eButtonPressed();
}

fun \Z test_event()
{
    Desktop desk=Desktop::GetDesktop();
    GUIContainer mw=GUIContainer(desk);
//	mw.eAdd(mb1)
//	mw.eAdd(mb2)



    GUIButton mb1=MyButton(mw); //MyButton is event singular, mb1 is a channel to the singular (which is 'write' only)
    MyButton mb2=MyButton(mw); //MyButton is event singular, mb is a channel to the singular

    //ok???
    Timer<MyButton> mt=Timer<MyButton>(10,timer_fun,mb1);//press every 10 sec


    //no order on events GUARANTIED!!!! (event WILL be send somewhen after expression is evaluated)
    //-> the ONLY way to set an event in a determined state is to create a new eventobj!
    //if an eventobj is used (e.g. flag nodes in cyclic graph) -> a new event obj is required for
    //each parallel run on the graph structure! (which makes sense!!)
    mb1->eSetText("Press e!"); //use -> for events (to make it more explicit)
    mb2->eSetText("Don't press me!!"); //(statement is bound to function!)


    //event obj can be read with temporal order using dependencies
    //required e.g. to draw hierachy of gui controls in back to front order

    \N state,state2;
    state = mb2.GetState();

    state2 = mb2.GetState(state);

    //NO COPYING UNIQUE TYPES!!!

    //event funs are read only->object does not change! (true function)
    //some_state is captured somewhen (can be before or after any of the prev ones)
    \N some_state=mb2.GetState();

    //mb2.SetCoords(...)//:error:private+changes state

    /*
            get DrawPrim somehow:
    */

    DrawPrim dp = DrawPrim::GetDP();

    dp'init = dp >>> mb1.Init() //special operator, pass as last argument(s), left associative!!
                 >>> mb2.Init();

    dp'after=dp'init >>> mb1.Draw()
                		 >>> mb2.Draw(); //is short for:
    //dp'after=mb2.Draw(mb1.Draw(dp))//can return locally processed data 

}

}
		
