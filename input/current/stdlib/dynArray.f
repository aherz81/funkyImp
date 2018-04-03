package stdlib;

import domains.take;

//option T& only has a meaning in member decls and arrays of non-primitive element types


public unique class dynArray<T> extends Array<T>
{
    //unique T& data[one_d{-1}];
    //int items;

    public dynArray(T& input[one_d{-1}])
    {
        //must copy:
        super(new T&[one_d{input.size[0]}].\[i]{input[i]},input.size[0]);
        //this.data=new T&[one_d{input.size[0]}].\[i]{input[i]};
        //this.items=input.size[0];
    }

    public dynArray(unique T& data[one_d{-1}],int items)
    {
        super(data,items);
        //this.data=data;
        //this.items=items;
    }

    public dynArray()
    {
        super(new T&[one_d{2}],0);
        //data=new T&[one_d{2}];
        //items=0;
    }

    dynArray<T> replace(int index,T obj)
    {
        cancel this where data[index]=obj;
    }

    dynArray<T> replace(int xx,T obj, out T old)
    {
        old=data[xx];
        cancel this where data[xx]=obj;
    }

    dynArray<T> size(out int s)
    {
        s=items;
        cancel this where items=s;
    }

    //artificial template param, so that method is only instantiated if used, cannot use this method when T is unique!
    <G> dynArray<T> get(int index,out T obj)
    {
        obj=data[index];
        cancel this;
    }

    dynArray<T> append(T obj)
    {
        if(items+1<data.size[0])
                data__next = data;
        else
                data__next = data.resize(data.size[0]*2);

        data__ins=data__next where data__next[items]=obj;

        cancel this where {data=data__ins;items=items+1;};
    }

    dynArray<T> removeLast(out T obj)
    {
        this__next=replace(items-1,(T)null,obj);

        if(items-1>this__next.data.size[0]/2)
                data__next=this__next.data;
        else
                data__next=this__next.data.resize(data.size[0]/2);

        cancel this__next where {this__next.data=data__next;this__next.items=items-1;};
    }

    dynArray<T> reverse()
    {
        if(items>1)
            cancel new dynArray<T>(data.take{data.size[0]}(items).one_d.\[i]{data[items-1-i]});
        else
            cancel this;
    }

    T&[one_d{-1}] getData()
    {
        cancel data.resize(items);
    }
}