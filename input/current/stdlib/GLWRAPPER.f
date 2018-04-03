package stdlib;

import ffi.Xlib;
import ffi.freeglut;

public unique class GLWRAPPER
{
    thread ogl;

	public static thread(ogl) GLWRAPPER Init(inout unique int argc,inout unique String[one_d{4}] argv)
	{
	    Xlib.XInitThreads():: //must call this on linux as ogl may use X and not all x interactions are synchronized (e.g. printf)
		freeglut.glutInit(argc,argv);
		finally null;
	}

	public static thread(ogl) GLWRAPPER Get()
	{
	    cancel null;//pseudo object
	}
	
	public static thread(ogl) void Redisplay()
	{
	    freeglut.glutPostRedisplay();
	}

	public thread(ogl) GLWRAPPER KeyboardFunc(void callback(unsigned char key,int x, int y)) { freeglut.glutKeyboardFunc(callback); finally this;}

	public thread(ogl) GLWRAPPER DisplayFunc(void callback()) { freeglut.glutDisplayFunc(callback); finally this;}
	public thread(ogl) GLWRAPPER Swap() { freeglut.glutSwapBuffers(); finally this;}

	public thread(ogl) GLWRAPPER InitDisplayMode(long flags) { freeglut.glutInitDisplayMode(flags); finally this;}
	public thread(ogl) GLWRAPPER InitWindowSize(long width, long height) { freeglut.glutInitWindowSize(width,height); finally this;}
	public thread(ogl) GLWRAPPER InitWindowPosition(long x, long y) { freeglut.glutInitWindowPosition(x,y); finally this;}
	public thread(ogl) GLWRAPPER CreateWindow(String name) { freeglut.glutCreateWindow(name); finally this;}

	public thread(ogl) GLWRAPPER Begin(int i) { freeglut.glBegin(i); finally this;}
	public thread(ogl) GLWRAPPER End() { freeglut.glEnd(); finally this;}
	public thread(ogl) GLWRAPPER Vertex2f(double v1,double v2) { freeglut.glVertex2f(v1,v2); finally this;}
	public thread(ogl) GLWRAPPER Color3f(double v1,double v2,double v3) { freeglut.glColor3f(v1,v2,v3); finally this;}

	public thread(ogl) GLWRAPPER ClearColor(double c0, double c1, double c2, double c3) { freeglut.glClearColor(c0,c1,c2,c3); finally this;}
	public thread(ogl) GLWRAPPER Flush() { freeglut.glFlush(); finally this;}
	public thread(ogl) GLWRAPPER Clear(int i) { freeglut.glClear(i); finally this;}

	public thread(ogl) GLWRAPPER MatrixMode (int i) { freeglut.glMatrixMode(i); finally this;}
	public thread(ogl) GLWRAPPER LoadIdentity () { freeglut.glLoadIdentity(); finally this;}
	public thread(ogl) GLWRAPPER Ortho(double v0, double v1, double v2, double v3, double v4, double v5) { freeglut.glOrtho(v0,v1,v2,v3,v4,v5); finally this;}

	public static void MainLoopEvent(){freeglut.glutMainLoopEvent();}

}
