// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;
import stdlib.vector;
import stdlib.gVector;

#define MATRIX_SIZE 4096
#define BLOCK_SIZE 32

#define INNER_BLOCK
//#define BLOCK
//#define MATMULT

//#define DUMP

#define TYPE float

domain two_d{x,y}:one_d{x*y}(o) = { (j,k) | j<x & k<y } //two free

//stupid defs cause pp doesn__t replace everything as it should do...
#define wrapper(n,a) n##a
#define wrapper2(n,a,b) n##a##b

//avoid non-linear constraints by substituting some constants early on:
#define region(dx,dy) wrapper2(region,dx,dy)
#define declregion(dx,dy) region(dx,dy){x,y}(i,j):two_d{dx,dy}(q,w) = { two_d{x,y}(a,b) | a>=i*dx & a<i*dx+dx & b>=j*dy & b<j*dy+dy & q=a & w=b }

domain declregion(BLOCK_SIZE,BLOCK_SIZE)

#define slicey(dy) wrapper(slicey,dy) //block row
#define declslicey(dy) slicey(dy){x,y}(i):two_d{x,dy}(q,w) = { two_d{x,y}(a,b) | a>=i*dy & a<i*dy+dy & q=a & w=b }

domain declslicey(BLOCK_SIZE)

#define slicex(dx) wrapper(slicex,dx) //block col
#define declslicex(dx) slicex(dx){x,y}(j):two_d{dx,y}(q,w) = { two_d{x,y}(a,b) | b>=j*dx & b<j*dx+dx & q=a & w=b }

domain declslicex(BLOCK_SIZE)


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



public class cur
{
	static TYPE[two_d{-1,-1}] transposeRRR(TYPE ma[two_d{-1,-1}])
	{
		cancel ma.\[r,c]{ma[c,r]};
	}

	static TYPE dotRR(TYPE r[row{-1,-1}],TYPE c[row{-1,-1}])
	{
		cancel r.one_d.\reduce{0}(i,accum) {accum + r.one_d[i] * c.one_d[i]};
	}

	static TYPE[two_d{-1,-1}] matmultRRR(TYPE ma[two_d{-1,-1}],TYPE mb[two_d{-1,-1}])
	{
		//iter over all indices and calc corresponding dot product
		cancel
				ma.\region(BLOCK_SIZE,BLOCK_SIZE){ma.size[0],ma.size[1]}(br,bc)
				{

					ma.region(BLOCK_SIZE,BLOCK_SIZE){ma.size[0],ma.size[1]}(br,bc)
					.two_d.\[r,c]
					{
						dotRR(ma.row{ma.size[0],ma.size[1]}(br*BLOCK_SIZE+r),mb.row{ma.size[0],ma.size[1]}(bc*BLOCK_SIZE+c))
						//((gVector<MATRIX_SIZE,TYPE,TYPE[row{MATRIX_SIZE,MATRIX_SIZE}]>)ma.row(br*BLOCK_SIZE+r))*((gVector<MATRIX_SIZE,TYPE,TYPE[row{MATRIX_SIZE,MATRIX_SIZE}]>)mb.row(bc*BLOCK_SIZE+c))
						//vector<MATRIX_SIZE>.dot(ma.row(br*BLOCK_SIZE+r),mb.row(bc*BLOCK_SIZE+c))
						//(br*BLOCK_SIZE+r)*BLOCK_SIZE+bc*BLOCK_SIZE+c
					}

				};
	}

    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
		TYPE matrix[two_d{-1,-1}]=new TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { 10000000.f/(r*MATRIX_SIZE+c+1) };

		matrix__b = new TYPE[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { 10000000.f/(c*MATRIX_SIZE+r+1) };

		matrix__btrans=transposeRRR(matrix__b);

		matrix__res=matmultRRR(matrix,matrix__btrans); //blocked version

		stdio.printf("matrix__res[%d,%d]=%f\n",(MATRIX_SIZE/2),(MATRIX_SIZE/2),matrix__res[(MATRIX_SIZE/2),(MATRIX_SIZE/2)],matrix__res[(MATRIX_SIZE/2),(MATRIX_SIZE/2)]);

		TYPE sum=matrix__res.one_d.\reduce{0}(i,accum){accum+matrix__res.one_d[i]};
		stdio.printf("sum %f",sum);

#ifdef DUMP
		matrix.\[r,c]{stdio.printf("matrix[%d,%d]=%f (%d)\n",r,c,matrix[r,c],matrix[r,c])};
		matrix__b.\[r,c]{stdio.printf("matrix__b[%d,%d]=%f (%d)\n",r,c,matrix__b[r,c],matrix__b[r,c])};
		matrix__btrans.\[r,c]{stdio.printf("matrix__btrans[%d,%d]=%f (%d)\n",r,c,matrix__btrans[r,c],matrix__btrans[r,c])};
		matrix__res.\[r,c]{stdio.printf("matrix__res[%d,%d]=%f (%d)\n",r,c,matrix__res[r,c],matrix__res[r,c])};
#endif

#ifdef TESTOP
		TestOP a=new TestOP();
		TestOP b=new TestOP();

		int res=a*b;
#endif

		finally 0; //use finally and don__t add extra wait?? does work with root??
    }
}

