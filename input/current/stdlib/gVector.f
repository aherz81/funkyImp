package stdlib;

import ffi.math; //sqrt
import stdlib.vector;

//extends A, so that we can build gVector of projection
public class gVector<X,T,A/* extends T[one_d{X}]*/> extends A
{
//    public static unique <T,A extends T[one_d{X}],B extends T[one_d{X}]>
//    A add(A r,B c)

    public inline <B extends T[one_d{X}]>
	T operator*(B b)
    {
        cancel vector<X>.dot(this,b);
    }

    public inline unique <B extends T[one_d{X}]> //let__s be more lenient here, we can take anything that__s a one_d of proper size and type
	gVector<X,T,A> operator-(B b)
    {
        cancel vector<X>.sub(this,b);
    }

    public inline unique <B extends T[one_d{X}]> //let__s be more lenient here, we can take anything that__s a one_d of proper size and type
	gVector<X,T,A> operator+(B b)
    {
        cancel vector<X>.add(this,b);
    }

    public inline unique gVector<X,T,A> operator*(T val)
    {
        cancel vector<X>.scale(this,val);
    }

    public inline unique gVector<X,T,A> operator-()
    {
        cancel this*(T)(-1);
    }

    public inline T length()
    {
        cancel math.sqrt(this*this);
    }

    public inline T lengthSquare()
    {
        cancel this*this;
    }
    
    public inline void dump(String format)
    {
        this.one_d.\[i]{stdio.printf(format,this.one_d[i],i)};
    }
        
}

