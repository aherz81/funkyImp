package ffi;

/*
WARNING: the compiler uses this file to parse cloog generated code (which generates stuff like max/min/etc.) DO NOT MODIFY THIS FILE
*/

public native class math
{
    public static <T> T sqrt(T val);
	public static <T> T abs(T val);
	public static <T> T ceild(T a, T b);
	public static <T> T floord(T a, T b);
	public static <T> T min(T a, T b);
	public static <T> T max(T a, T b);
        public static <T,Z> T pow(T base, Z exponent);
}
