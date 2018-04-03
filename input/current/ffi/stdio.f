package ffi;

//FFI: (native=no output, include header <stdio>)
public native unique class stdio
{
	public static int SEEK_SET;
	public static int SEEK_CUR;
	public static int SEEK_END;

    public static FILE fopen(String f,String m); //thread safe method fopen, String==char*
    public static void fprintf(inout FILE f, String txt, Object ... j); //inout means var is read and written
    public static <T> void fread(inout unique T data[],long size,long items, inout FILE f);
	public static <T> void fread(out T data,long size,long items, inout FILE f);
    public static <T> void fwrite(T data[],long size,long items, inout FILE f);
    public static <T> void fwrite(out T data,long size,long items, inout FILE f);//out for data is hack here to force passing as pointer
	public static int fseek(inout FILE f,long offset,int origin);
    public static void fclose(inout FILE f);
    public static long ftell(inout FILE f);
    public static void printf(String txt,Object ... j); //HACK!
	public static void fflush(inout FILE f);
	public static FILE& stdin;
	public static FILE& stdout;
}

