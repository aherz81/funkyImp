import ffi.stdio;
import ffi.math;
import ffi.stdlib;
import ffi.FILE;

import stdlib.iMatrix;
import stdlib.gVector;
import stdlib.File;

import domains.two_d;
import domains.linalg.row;
import domains.linalg.col;

// General
#define RAND_MAX 32767
#define TYPE double
// General Data
#define DIM 2
// Lennard Jones Potential
#define LJ_RM 2
#define LJ_E 1.5
#define DELTA 0.001
#define EPS 0.001
#define MASS 1.0

#define OUTPUT "output.xyz"
#define BORDER 10
#define NUM_GRIDCELLS 2
#define GRID_SIZE 5
#define NUM_MAXATOMS 2

//#define NUM_ATOMS_SQUARE 9
//#define NUM_ATOMS 128
//#define NUM_ITERATIONS 200

// Just making the source readable

#define VecATOMS_row gVector<DIM, TYPE, TYPE[row{NUM_ATOMS,DIM}]>
#define VecATOMS_col gVector<NUM_ATOMS, TYPE, TYPE[col{NUM_ATOMS,DIM}]>
#define VecDIS_row  gVector<DIM, TYPE, TYPE[row{NUM_ATOMS_SQUARE,DIM}]>

#define MatAxD iMatrix<NUM_ATOMS, DIM, TYPE>

#define MatDIS iMatrix<NUM_ATOMS_SQUARE, DIM, TYPE>

#define MatGRID iMatrix<NUM_GRIDCELLS, NUM_MAXATOMS, TYPE>

public class cur {

    // Static Value can be calculated before runtime
    static TYPE LJ_RM_6 = math.pow(LJ_RM, 6);
    static TYPE LJ_RM_12 = math.pow(LJ_RM, 12);
    static TYPE FMA = (DELTA * DELTA) / MASS;

    static int main(int argc, inout unique String[one_d { -1}] argv) {

        MatGRID grid = new MatGRID();

        MatAxD atoms = new iMatrix<NUM_ATOMS, DIM, TYPE>().\[x, y] { x + y }; //BORDER * (stdlib.rand() * 1.0) / RAND_MAX * 1.0};
   
        iterate(atoms);
        
        finally 0;
    }

/***********************************************
 *                   Loop                      *
 ***********************************************/
    static MatAxD iterate(MatAxD atoms, MatAxD oldVel, int num) {

        if (num > NUM_ITERATIONS){
            cancel atoms;
        } else {

            MatDIS distances = calcDistances(atoms);
            MatDIS ljp = getLJPMatrix(distances);
            MatAxD velocity = getVelocities(ljp, oldVel);
            MatAxD result = addVelocity(atoms, velocity);

            writeFile(result);

            cancel iterate(result, velocity, num + 1);
        }
    }


/***********************************************
 *                 Functions                   *
 ***********************************************/
    static int[one_d{-1}] fillGrid(MatAxD atoms, int x, int y, int curr){ // Parallelisierbar !
        if(curr == NUM_ATOMS - 1){
            if(testCell(x,y, atoms.rowVec(curr)))
                cancel new int[one_d{1}].\[i] { curr };
            else
                cancel new int[one_d{1}].\[i] { -1 };
        } else {
            if(testCell(x,y, atoms.rowVec(curr))){
                int[one_d{-1}] arr = fillGrid(atoms, x, y, curr + 1);
                int length = arr.size[0];
                if(length == 1 && arr[0] == -1){
                    cancel new int[one_d{1}].\[i] { curr };
                } else {
                    cancel new int[one_d{1 + length}].\[i] {
                        if(i < length){ arr[i] } else { curr }
                    };
                }
            } else {
                cancel fillGrid(atoms, x, y, curr + 1);
            }
        }
    }

    static MatDIS calcDistances(MatAxD mat)
    {
        cancel new MatDIS().\row(i) {
            MatDIS.row(i).one_d.\[j]{
               (mat.rowVec(getRow(i))).one_d[j] - (mat.rowVec(getCol(i))).one_d[j]
            }
        };
    }

    // Returns a NUM_ATOMS x NUM_ATOMS matrix with the forces calculated using the LJP
    // F_i,j is the Force between Atom i and Atom j
    static MatDIS getLJPMatrix(MatDIS dist)
    {
        cancel new MatDIS().\row(i) {
            MatDIS.row(i).one_d.\[j]{
                calcForce(dist.rowVec(i)).one_d[j] // ROW zurückgeben
            }
        };
    }

    // LJP formula
    static VecDIS_row calcForce(VecDIS_row vec) {
        TYPE length = getLength(vec);
        cancel vec * ( length * -12 * LJ_E * ( LJ_RM_6 * math.pow(length, 7) + LJ_RM_12 * math.pow(length , 13)));
    }

    static MatAxD addVelocity(MatAxD atoms, MatAxD vel){
        cancel new MatAxD().\row(i){
            MatAxD.row(i).one_d.\[j]{
                atoms.row(i).one_d[j] + vel.row(i).one_d[j] // AUF ROW TESTEN
            }
        };
    }

    static MatAxD getVelocities(MatDIS dis, MatAxD oldVel)
    {
        cancel new MatAxD().\row(i) {
            MatAxD.row(i).one_d.\[j]{
                oldVel[i,j] + addUpLJP(dis, i).one_d[j] * FMA // UMBAUEN AUF In der Funktion
            }
        };
    }

/***********************************************
 *                  Helper                     *
 ***********************************************/
    // Overload
    static inline MatAxD iterate(MatAxD atoms) {
        cancel iterate(atoms, new MatAxD(), 0);
    }

    // Overload
    static inline VecDIS_row addUpLJP(MatDIS dis, int i)
    {
        cancel addUpLJP(dis, i, 0);
    }

    static inline boolean testCell(int x, int y, VecATOMS_row atom)
    {
        cancel x == ((int)atom.one_d[0] / GRID_SIZE) &&  y == ((int)atom.one_d[1] / GRID_SIZE);
    }

    // Returns the 1 devided by the Distance to normalize the vectors later on
    // LJP also needs 1 / Distance
    static TYPE getLength(VecDIS_row vec){
        // TYPE length = vec.length();
        TYPE length = math.sqrt(vec.one_d[0] * vec.one_d[0] + vec.one_d[1] * vec.one_d[1]);
        if (length <= EPS) {
            cancel 0;
        } else {
            cancel 1 / length;
        }
    }
    static VecDIS_row addUpLJP(MatDIS dis, int i, int curr) // AUF TAKE umbauen
    {
        if(curr == NUM_ATOMS - 1){
		//stdio.printf("(%f, %f)\n", dis.rowVec(i * NUM_ATOMS + curr).one_d[0], dis.rowVec(i * NUM_ATOMS + curr).one_d[1]);
            cancel dis.rowVec(i * NUM_ATOMS + curr);
	}
        else
            cancel dis.rowVec(i * NUM_ATOMS + curr) + addUpLJP(dis, i, curr + 1);
    }

    // 1D -> 2D
    static inline int getRow(int i){
        cancel i / NUM_ATOMS;
    }
    // 1D -> 2D
    static inline int getCol(int i){
        cancel i - (getRow(i) * NUM_ATOMS);
    }
    // 2D -> 1D
    static inline int getIndex(int y, int x){
        cancel y * NUM_ATOMS + x;
    }

/***********************************************
 *                   I/O                       *
 ***********************************************/
    // Clear the File and write the fist Line
    static void initFile(MatAxD atoms) {
        FILE handle = stdio.fopen(OUTPUT, "w");
        stdio.fprintf([handle'1=handle],"%d\n(0,0)AtomConfig\n", NUM_ATOMS);
        handle'2 = writeRec(atoms, handle'1, 0);
        stdio.fclose([handle'close = handle'2]);
        // stdio.printf("%s created\n",OUTPUT);
    }

    // Wrapper to write to the File
    static void writeFile(MatAxD atoms) {
        FILE handle = stdio.fopen(OUTPUT, "a");
        stdio.fprintf([handle'1=handle],"%d\n(0,0)AtomConfig\n", NUM_ATOMS);
        handle'2 = writeRec(atoms, handle'1, 0);
        stdio.fclose([handle'close = handle'2]);
        // stdio.printf("Update written to %s\n",OUTPUT);
    }

    // Recursive Method to write all atoms
    static FILE writeRec(MatAxD atoms, FILE handle, int pos) {
        if (pos == NUM_ATOMS) {
            cancel handle;
        } else {
            handle'res = writeAtom(atoms.rowVec(pos), handle);
            cancel writeRec(atoms, handle'res, pos + 1);
        }
    }

    // Wrapper for writing an atom to the File
    static FILE writeAtom(VecATOMS_row atoms, FILE handle) {
        stdio.fprintf([handle'ret = handle], "H\t%f\t%f\t%d\n", atoms.one_d[0], atoms.one_d[1], 0);
        cancel handle'ret;
    }
}

// VERGLEICHEN
// RAUSSCHREIBEN DATEN AUSSCHALTEN
