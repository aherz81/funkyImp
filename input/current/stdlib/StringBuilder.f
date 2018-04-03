package stdlib;

import stdlib.dynArray;

native class global //special: global will not be included
{
    static public char getChar(String s,int offset); //defined in runtime so we can access characters
}

public unique class StringBuilder
{
    private dynArray<char> data;

    public StringBuilder(){data=new dynArray<char>();}

    private StringBuilder add(String s,int c)
    {
        cancel this where data=data.append(global.getChar(s,c));
    }

    private nonblocking StringBuilder addAll(String s,int c)
    {
        if(global.getChar(s,c)==0)
            cancel this;

        this__next=this.add(s,c);
        finally this__next.addAll(s,c+1);
    }

    public StringBuilder(String s)
    {
        data=new dynArray<char>();
        addAll(s,0);
    }

    public String get()
    {
        cancel (String)add("",0).data.getData();
    }

    public StringBuilder add(String s)
    {
        cancel addAll(s,0);
    }

    private static String lookupDigit(int d)
    {
        if(d==0) //ugh..maybe we do need a switch :)
            cancel "0";
        if(d==1)
            cancel "1";
        if(d==2)
            cancel "2";
        if(d==3)
            cancel "3";
        if(d==4)
            cancel "4";
        if(d==5)
            cancel "5";
        if(d==6)
            cancel "6";
        if(d==7)
            cancel "7";
        if(d==8)
            cancel "8";
        if(d==9)
            cancel "9";

        finally "?";
    }

    private nonblocking StringBuilder addInt(int val)
    {
        if(val==0)
            cancel this;
        else
        {
            this__next=add(lookupDigit(val%10),0);
            cancel this__next.addInt(val/10);
        }
    }

	private StringBuilder addIntWrapper(int val)
	{
		StringBuilder res;
		if(val<0)
        {
            StringBuilder tmp=(new StringBuilder()).addInt(-val);
            res=tmp.add("-",0);
        }
        else
        {
            res=(new StringBuilder()).addInt(val);
        }

        finally res where res.data=res.data.reverse();
	}

    public StringBuilder add(int val)
    {

        if(val==0)
            cancel add("0");
        else
			cancel add(addIntWrapper(val).get());
    }

    public StringBuilder operator+(String s)
    {
		cancel add(s);
    }

    public StringBuilder operator+(int v)
    {
		cancel add(v);
    }
}

