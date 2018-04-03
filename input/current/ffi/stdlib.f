package ffi;

public native class stdlib
{
    public static int rand();
    public static void srand(int seed);
    public static int atoi(String s);
}

