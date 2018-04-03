package stdlib;

public class Array<T>
{
    T& data[one_d{-1}];
    int items;
    
    public Array(T& data[one_d{-1}],int items)
    {
        this.data=data;
        this.items=items;
    }
 
    T&[one_d{-1}] getData()
    {
        cancel data;
    }    
    
    Array<T> size(out int s)
    {
        s=items;
        cancel this;
    }

    //artificial template param, so that method is only instantiated if used, cannot use this method when T is unique!
    <G> Array<T> get(int index,out T obj)
    {
        obj=data[index];
        cancel this;
    }
}

