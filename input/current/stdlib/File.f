package stdlib;

import ffi.stdio;
import ffi.FILE;

//class File with unique attribute=only one handle allowed on this class, may contain unique members
public unique class File
{
    FILE& handle; //must be option (unknown object), is unique

	public File size(out long size)
	{
		long pos;
		File cur=pos(pos);
		cur__end=cur.seek(0,stdio.SEEK_END); //FIXME: should error if cur is missing here??
		cur__pos=cur__end.pos(size);
		cur__reset=cur__pos.seek(pos,stdio.SEEK_SET);
		finally cur__reset;
	}

    public File(String name,String mode)
    {
        handle=stdio.fopen(name,mode);
    }
/*
	public File isValid(out boolean valid)
	{
		valid=handle!=null;
		finally this where handle=handle;
	}
*/
    public File write(String s)
    {
        stdio.fprintf([handle__next=handle],s); //handle is read, handle__next is written to (same type as handle)
        finally this where handle=handle__next; //only on linear classes, unique only members of
    }

    public File pos(out long length)
    {
        length = (int)stdio.ftell([handle__next=handle]);
        finally this where handle=handle__next;
    }

	public File seek(long offset, int origin)
	{
		stdio.fseek([handle__next=handle],offset,origin);
		finally this where handle=handle__next;
	}

    public <T> File read(long count,out T outdata[one_d{-1}])
    {
		//unique T marray[one_d{-1}]=new T[one_d{count}];
		unique T native_array[]=new T[count]; //create native array
        stdio.fread([native_array__next=native_array],count,sizeof(T),[handle__next=handle]);
		outdata=(T[one_d{count}])native_array__next;
        finally this where handle=handle__next; //only on linear classes, unique only members of
    }

	public <T> File read(out T data) //read single datum
	{
		stdio.fread(data,sizeof(T),1,[handle__next=handle]);
        finally this where handle=handle__next; //only on linear classes, unique only members of
	}

    public <T> File write(T data[one_d{-1}])
    {
		T native_array[]=(T[])data; //convert to native array
        stdio.fwrite(native_array,data.size[0],sizeof(T),[handle__next=handle]);
        finally this where handle=handle__next; //only on linear classes, unique only members of
    }

	public <T> File write(T data) //unique single datum
	{
		unique T copy=data;//hack to pass data as pointer..
		stdio.fwrite(copy,sizeof(T),1,[handle__next=handle]);
        finally this where handle=handle__next; //only on linear classes, unique only members of
	}

    public File close()
    {
		stdio.fclose([handle__next=handle]);
		finally this where handle=null;
    }
}
