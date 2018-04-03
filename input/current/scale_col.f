// @PARAM: -PP "" -g:none -CPP -d ./tmp -cp ../../../../../../input/current/
// shows how to run a custom shell script as a test:
// @CUSTOM ./custom.sh
// as can be seen from the cmdl, the CWD is set to the .funky file
// @TEST noerror


import ffi.stdio; //printf
import stdlib.vector;
import domains.two_d;
import domains.linalg.*;

//unique versions
	//practically a typedef

unique class uVector<X,T> extends iVector<X,T>{} //must be a template to get a unique version!

	//this will instantiate unique iMatrix<X,Y,T> as base class which in turn will instantiate matrix<X,Y>.mult(this,b) with unique this (in place update)

unique class uMatrix<X,Y,T> extends iMatrix<X,Y,T>{}

//what are unique doms good for?:
//- partial update
//- memory reuse

#define MS 2048

public class cur
{
    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
		int mat[two_d{16,16}]=null;

		vector<16>.scale(mat.col(1),2);

		finally 0; //use finally and don__t add extra wait?? does work with root??
    }
}
