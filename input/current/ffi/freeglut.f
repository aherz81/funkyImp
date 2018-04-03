package ffi;

//stupidly ogl handles it__s context implicitly (via current thread) so we must guarantee that all funs that access the context are executed on the same thread

public native class freeglut
{
    thread ogl;
	public static thread(ogl) void glutKeyboardFunc(void callback(unsigned char key,int x, int y));
	public static thread(ogl) void glutDisplayFunc(void callback());

	public static thread(ogl) void glutInitDisplayMode(long flags);
	public static thread(ogl) void glutInitWindowSize(long width, long height);
	public static thread(ogl) void glutInitWindowPosition(long x, long y);
	public static thread(ogl) void glutCreateWindow(String name);
	public static thread(ogl) void glutInit(inout unique int argc,inout unique String[one_d{4}] argv);

	public static thread(ogl) void glBegin(int i);
	public static thread(ogl) void glEnd();
	public static thread(ogl) void glVertex2f(double v1,double v2);
	public static thread(ogl) void glColor3f(double v1,double v2,double v3);

	public static thread(ogl) void glClearColor(double c0, double c1, double c2, double c3);
	public static thread(ogl) void glFlush();
	public static thread(ogl) void glClear(int i);

	public static thread(ogl) void glMatrixMode (int i);
	public static thread(ogl) void glLoadIdentity ();
	public static thread(ogl) void glOrtho(double v0, double v1, double v2, double v3, double v4, double v5);

	public static thread(ogl) void glutMainLoopEvent();
	public static thread(ogl) void glutPostRedisplay();
	public static thread(ogl) void glutSwapBuffers();
}
