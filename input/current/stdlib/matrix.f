package stdlib;

import domains.two_d;
import domains.linalg.*;

public class matrix<X,Y>
{
    public inline static unique <T,A extends T[two_d{X,Y}],B extends T[two_d{Y,X}]>
    T[two_d{X,X}] mult(A a,B b)
    {
        cancel a.\[i,j]
		{
			vector<X>.dot(a.row(i),b.col(j))
		};
    }

	//TRANSPOSED: X*Y x X*Y = Y*Y
    public inline static unique <T,A extends T[two_d{X,Y}],B extends T[two_d{X,Y}]>
    T[two_d{X,X}] multTrans(A a,B b)
    {
        cancel a.\[i,j]{vector<X>.dot(a.row(i),b.row(j))};
    }

    public inline static unique <T,A extends T[two_d{X,Y}],B extends T[one_d{X}]>
    T[one_d{Y}]	mult(A a,B b)
    {
        cancel new T[one_d{Y}].\[i]{vector<X>.dot(a.row(i),b)};
    }

    public inline static unique <T,A extends T[two_d{X,Y}]>
    T[two_d{Y,X}]	transpose(A a)
    {
        cancel new T[two_d{Y,X}].\[r,c]{a.two_d[c,r]};
    }

}
