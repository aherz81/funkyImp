import ffi.stdio;
import stdlib.*;

#define dim 2
#define num_Particles 9

#define mass 1.0
#define charge 1.0

public class Particle
{
    double Q;    //Electric Charge in Coloumb
    double m;     //Mass in kg
    int id;

    iVector<dim,double> pos;/* Position in meters. x ans y */
    iVector<dim,double> v;     /* velocity in meters/second. x and y direction */

    Particle(int nid, double nQ, double nM, iVector<dim,double> npos,
         iVector<dim,double> nv)
    {
        Q = nQ;
        m = nM;
        pos = npos;
        v = nv;
        id = nid;
    }

    double fill(int i, double nx, double ny)
    {
        if( i == 0)
            cancel nx;
        else
            cancel ny;
    }

    Particle(int nid, double nQ, double nM, double nx, double ny,
         double nvx, double nvy)
    {
        Q = nQ;
        m = nM;
        pos = new double[one_d{dim}].\[i]{ fill(i, nx, ny) };
        v = new double[one_d{dim}].\[i]{ fill(i, nvx, nvy) };
        id = nid;
    }

    /*
     * @F: the Force on @this in Newton
     * @t: amount of time @F influences @this in seconds
     */
    Particle calc(iVector<dim,double> F, double t)
    {
        /* F = m * a => a = F/m */
        iVector<dim,double> a = F * (1/m);

        /*stdio.printf("1/m: %f\n", 1/m);
        a.\[i]{stdio.printf("a: %f\n", a[i]);};
        F.\[i]{stdio.printf("F: %f\n", F[i]);};*/

        /* pos(t) = a/2 * t^2 + v * t + pos */
        iVector<dim,double> post = a * 0.5 * t * t +
                       v * t + pos;
        /*post.\[i]{stdio.printf("post: %f\n", post[i]);};
        stdio.printf("t * t: %f", 0.5 * t * t);*/

        /* v(t) = a * t + v */
        iVector<dim,double> vt = a * t + v;

        cancel new Particle(id, Q, m, post, vt);
    }

    void print()
    {
        stdio.printf("p%d: charge = %f, mass = %f, x = %f, y = %f, vx = %f, vy = %f\n",
                id, Q, m, pos[0], pos[1], v[0], v[1]);
    }

    /*
     * Coulomb Force of @p on @this
     */
    iVector<dim,double> Coulomb_Force(Particle p)
    {
        iVector<dim,double> r_p_this = this.pos + (- p.pos);
        double r = r_p_this.length();

        if (r == 0.0) {
            iVector<dim,double> ret =
                    new double[one_d{dim}].\[i]{0.0};
            cancel ret;
        } else {
            cancel r_p_this * p.Q * this.Q * (1/(r*r*r));
        }
    }
}

public class cur
{
    static iVector<dim,double>
    _acum_force(Particle p, Particle[one_d{num_Particles}] particles, int i)
    {
        if(i == num_Particles)
            cancel new double[one_d{dim}].\[i]{0.0};

        finally p.Coulomb_Force(particles[i]) +
            _acum_force(p, particles, i + 1);
    }

    /*
     * Sum up the force on @p from @particles
     */
    static iVector<dim,double>
    acum_force(Particle p, Particle[one_d{num_Particles}] particles)
    {
        cancel _acum_force(p, particles, 0);
    }

    static Particle[one_d{num_Particles}]
    step(Particle[one_d{num_Particles}] p, double time)
    {
        Particle[one_d{num_Particles}] ret =
            new Particle[one_d{num_Particles}].\[i]{
                p[i].calc(acum_force(p[i], p), time)
            };
        cancel ret;
    }

    static Particle fill_Particles(int i)
    {
        cancel new Particle(i, charge, mass, i, i, 0, 0);
    }

    static nonblocking Particle[one_d{num_Particles}] simulate(Particle[one_d{num_Particles}] p,
                double time, int iterations)
    {
        if(iterations <= 0)
			cancel p;
		//else
		{
			p__ = step(p, time);
			p__.\[i]{p__[i].print()};
		}
		finally simulate(p__, time, iterations - 1);
    }

    static int main(int argc, inout unique String[one_d{-1}] argv)
    {
        Particle[one_d{num_Particles}] particles = new Particle[one_d{num_Particles}].\[i]{ fill_Particles(i) };

        particles.\[i]{particles[i].print()};

        particles__end=simulate(particles, 0.01, 2);

		particles__end.\[i]{particles__end[i].print()};

        finally 0;
    }
}