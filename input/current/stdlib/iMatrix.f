package stdlib;

import domains.two_d;
import domains.linalg.row;
import domains.linalg.col;
import domains.linalg.utriag;

import stdlib.iVector;
import stdlib.matrix;

public class iMatrix<X,Y,T> extends T[two_d{X,Y}]
{
    public inline unique <B extends iMatrix<Y,X,T>>
    iMatrix<Y,Y,T> operator*(B b)
    {
        cancel matrix<X,Y>.mult(this,b);
    }

    public inline unique <B extends iMatrix<X,Y,T>>
    iMatrix<X,Y,T> operator+(B b)
    {
        cancel matrix<X,Y>.multTrans(this,b);
    }

    public inline unique <B extends iMatrix<Y,X,T>>
    iMatrix<X,Y,T> operator/(B b)
    {
        cancel matrix<X,Y>.multTrans(this,!b);
    }

    public inline unique iMatrix<Y,X,T> operator!()
    {
        cancel matrix<X,Y>.transpose(this);
    }

    public inline unique <B extends iVector<X,T>> //FIMXE: allow one_d ?
    iVector<Y,T> operator*(B b)
    {
        cancel matrix<X,Y>.mult(this,b);
    }

    //accessors to cast simple projections into objects with operators
    //do not overwrite row/col or we get cyclic deps probs
    public inline gVector<Y,T,T[row{X,Y}]> rowVec(int i) //overwrites row from base class(array)
    {
        cancel super.row(i);//behold it__s a gVector<row> not an iVector or a gVector<col>
    }

    public inline gVector<X,T,T[col{X,Y}]> colVec(int i)
    {
        cancel super.col(i);
    }
    
    public void dumpRows(String format,String name)
    {
        this.\row(i){stdio.printf("%s.row(%d)=<",name,i)::this.rowVec(i).dump(format)::stdio.printf(">\n");};
    }

    public void dump(String format,String name)
    {
        this.\[i,j]{stdio.printf("%s[%d,%d]=",name,i,j)::stdio.printf(format,this[i,j])::stdio.printf("\n");};
    }
}


