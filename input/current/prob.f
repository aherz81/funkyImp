// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio; //printf
import stdlib.vector; //printf
import domains.two_d;
import domains.linalg.col;
import domains.linalg.row;
import domains.linalg.trace;

//unique versions
	//practically a typedef

//unique class uVector<X,T> extends iVector<X,T>{} //must be a template to get a unique version!

	//this will instantiate unique iMatrix<X,Y,T> as base class which in turn will instantiate matrix<X,Y>.mult(this,b) with unique this (in place update)

//unique class uMatrix<X,Y,T> extends iMatrix<X,Y,T>{}

//what are unique doms good for?:
//- partial update
//- memory reuse

#define MS 256

public class matmult
{
    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
		int flat_matrix[one_d{-1}]=new int[one_d{16}].\[i]{i+1};

		flat_matrix.trace{4,4}().\[x,y]{stdio.printf("dyn_trace[%d,%d]=%d\n",x,y,flat_matrix.trace{4,4}()[x,y])};

		int mat[two_d{10,10}]=null;

		int i=mat.col(1).one_d[0];

		mat.\col(j) {mat.col(j)};

		vector<10>.dot(mat.col(0),mat.col(1));
		vector<10>.dot(mat.row(0),mat.row(1));

		finally 0; //use finally and don__t add extra wait?? does work with root??
    }
}

