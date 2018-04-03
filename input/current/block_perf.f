// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio;

#define MATRIX_SIZE 2048

//#define BLOCK

domain two_d{x,y} = { (j,k) | j<x & k<y } //two free

//avoid non-linear constraints by substituting some constants early on:
#define region(dx,dy) region##dx##dy
#define declregion(dx,dy) region(dx,dy){x,y}(i,j):two_d{dx,dy}(q,w) = { two_d{x,y}(a,b) | a>=i*dx & a<i*dx+dx & b>=j*dy & b<j*dy+dy & q=a & w=b }

domain declregion(8,8)

//domain inner_2d{x,y} = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1&b<y-1}
domain inner_2d{x,y}:one_d{(x-2)*(y-2)}(j,k) = { two_d{x,y}(a,b) | a>0 & b>0 & a< x-1& j=a & k=b}

domain range{y,ys,ye}:one_d{ye-ys}(j) = { one_d{y}(a) | ys<=a & a<ye & ye<=y & j=a } //one free (a)

//alex: static dom params __{}__ are optional:
domain row{x,y}(j):one_d{y}(k) = { two_d{x,y}(a,b) | a=j & k=b } //one free(b)
domain col{x,y}(j):one_d{x}(k) = { two_d{x,y}(a,b) | b=j & k=a }	//one free(a)

//should be error, cause one_d{x} and trace{x,y} don__t have identical shape:
domain trace{x,y}:one_d{x}(j) = { two_d{x,y}(a,b) | j=a & a=b & x=y }// one free (a), indep of j //no order on items!! (warn??)

domain utriag{x,y} = { two_d{x,y}(a,b) | b>=a+1 & x=y }
domain ltriag{x,y} = { two_d{x,y}(a,b) | b<a & x=y }
/*
- phd thesis : j. foster
strategie:
- user friendly (beispiele)
- theorie

dph (data parallel haskell)
cyclone (regions)

vgl. feather weight java/bocchino

mini lang/type sys

uniqueness
*/
public class cur
{

	static int dotRR(int r[row{MATRIX_SIZE,MATRIX_SIZE}],int c[row{MATRIX_SIZE,MATRIX_SIZE}])
	{
		cancel r.one_d.\reduce{0}(i,accum) {accum + r.one_d[i] * c.one_d[i]};
	}

	static int[two_d{MATRIX_SIZE,MATRIX_SIZE}] transposeRRR(int ma[two_d{MATRIX_SIZE,MATRIX_SIZE}])
	{
		int m[two_d{}]=ma.\[r,c]{ma[c,r]};
		cancel ma.\[r,c]{ma[c,r]};
	}
#ifdef BLOCK
	static int[two_d{MATRIX_SIZE,MATRIX_SIZE}] matmultRRR(int ma[two_d{MATRIX_SIZE,MATRIX_SIZE}],int mb[two_d{MATRIX_SIZE,MATRIX_SIZE}])
	{
		//iter over all indices and calc corresponding dot product
		cancel
				ma.\region(8,8)(br,bc)
				{
					ma.region(8,8)(br,bc)
					.two_d.\[r,c]
					{
						dotRR(ma.row(br*8+r),mb.row(bc*8+c))
						//(br*8+r)*16+bc*8+c
					}

				};
	}
#endif
	static int[two_d{MATRIX_SIZE,MATRIX_SIZE}] matmult(int ma[two_d{MATRIX_SIZE,MATRIX_SIZE}],int mb[two_d{MATRIX_SIZE,MATRIX_SIZE}])
	{
		//iter over all indices and calc corresponding dot product
		cancel ma.\[r,c]{dotRR(ma.row(r),mb.row(c))};
	}


    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
		int matrix[two_d{MATRIX_SIZE,MATRIX_SIZE}]=new int[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { r*MATRIX_SIZE+c };

		matrix__b = new int[two_d{MATRIX_SIZE,MATRIX_SIZE}].\[r,c] { c*MATRIX_SIZE+r };

#ifdef BLOCK
		matrix__block=matmultRRR(matrix,transposeRRR(matrix__b)); //blocked version
#endif
		matrix__block=matmult(matrix,transposeRRR(matrix__b));

		stdio.printf("matrix__block[%d,%d]=%d\n",(MATRIX_SIZE/2),(MATRIX_SIZE/2),matrix__block[(MATRIX_SIZE/2),(MATRIX_SIZE/2)]);

		matrix.\[r,c]{stdio.printf("matrix[%d,%d]=%d\n",matrix[r,c])};
		matrix__b.\[r,c]{stdio.printf("matrix__b[%d,%d]=%d\n",matrix__b[r,c])};
		matrix__block.\[r,c]{stdio.printf("matrix__block[%d,%d]=%d\n",matrix__block[r,c])};


		TYPE sum=matrix__block.\reduce{0}(i,j,accum){accum+matrix__block[i,j]};
		stdio.printf("sum %f",sum);

		finally 0; //use finally and don__t add extra wait?? does work with root??
    }
}
