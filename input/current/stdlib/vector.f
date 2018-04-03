package stdlib;

//generic vector class, works on anything compatible to one_d{X}
//take care: ret val type is given by first argument type
public class vector<X>
{
    public inline static <T,A extends T[one_d{X}],B extends T[one_d{X}]>
    T dot(A r,B c)
    {
        cancel r.one_d.\reduce{0}(i,accum) {accum + r.one_d[i] * c.one_d[i]};
    }

    public inline static unique <T,A extends T[one_d{X}],B extends T[one_d{X}]>
    A add(A r,B c)
    {
        cancel r.one_d.\[i] {r.one_d[i] + c.one_d[i]};
    }

    public inline static unique <T,A extends T[one_d{X}],B extends T[one_d{X}]>
    A sub(A r,B c)
    {
        cancel r.one_d.\[i] {r.one_d[i] - c.one_d[i]};
    }

    public inline static unique <T,A extends T[one_d{X}]>
    A scale(A a,T b)
    {
        cancel a.one_d.\[i]{a.one_d[i]*b};
    }
}
