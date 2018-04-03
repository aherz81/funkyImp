#include "main.h"

using namespace std;


PMV atoms[NUM_ATOMS];
Vector2 forces[NUM_ATOMS];
std::vector<int> grid[LIMITS_BOX / SIZE_CELL][LIMITS_BOX / SIZE_CELL];

int LJ_RM_6 = pow(LJ_RM, 6);
int LJ_RM_12 = pow(LJ_RM, 12);

int main()
{
#if GRID == true
	print("USING GRID\n");
#endif
	srand(time(NULL));

	GeneratePoints();
	WriteToXYZ();
#if GRID == true
	UpdateGrid();
#endif
	ClearXYZ();
	for (int i = 0; i < NUM_ITERATIONS; i++)
	{
#if GRID == true
		UpdateGrid();
#endif
		ClearForces();
		NonBondedForces();
		Interpolate();
		WriteToXYZ();

	}
	return 0;
}
#if GRID == true
void UpdateGrid()
{
	int prev_size = 0;
	for (int x = 0; x < LIMITS_BOX / SIZE_CELL; x++)
	{
		for (int y = 0; y < LIMITS_BOX / SIZE_CELL; ++y)
		{
			prev_size = grid[x][y].size();
			grid[x][y].clear();
			grid[x][y].reserve(prev_size);
		}
	}
	int x = 0 , y = 0;
	for (int i = 0; i < NUM_ATOMS; i++)
	{
		x = (atoms[i].x < 0 ? 0 : (int)atoms[i].x) % (int)LIMITS_BOX  / (int)SIZE_CELL;
		y = (atoms[i].y < 0 ? 0 : (int)atoms[i].y) % (int)LIMITS_BOX  / (int)SIZE_CELL;
		grid[x][y].push_back(i);
	}
}
#endif
void NonBondedForces()
{
#if GRID == true
	for (int x = 0; x < LIMITS_BOX / SIZE_CELL; x++)
	{
		for (int y = 0; y < LIMITS_BOX / SIZE_CELL; ++y)
		{
			std::vector<int> cells = GetSurroundingCells(x, y);
			int pos = 0, size = cells.size();
			for (int i = 0; i < size; i++)
			{
				for (int j = i + 1; j < size; j++)
				{
					Vector2 force = CalcForce(atoms[cells[i]], atoms[cells[j]]);
					forces[cells[i]] += force;
					forces[cells[j]] += -force;
				}
			}
		}
	}
#else
	for (int i = 0; i < NUM_ATOMS; i++)
	{
		for (int j = i + 1; j < NUM_ATOMS; j++)
		{
			if (i == j) {
				continue;
			}
			else {
				Vector2 force = CalcForce(atoms[i], atoms[j]);
				forces[i] += force;
				forces[j] += -force;
			}
		}
	}
#endif
}

void Interpolate()
{
	for (int i = 0; i < NUM_ATOMS; ++i)
	{	// Velocity
		atoms[i].V_x += (forces[i].x / atoms[i].m) * DELTA; //  v = ( a = F / m ) * DELTA
		atoms[i].V_y += (forces[i].y / atoms[i].m ) * DELTA;
		// Position
printf("(%f,%f)\n", atoms[i].V_x * DELTA, atoms[i].V_y * DELTA );
		atoms[i].x += atoms[i].V_x * DELTA; // x = v * t
		atoms[i].y += atoms[i].V_y * DELTA;
		ApplyBoundaries(i);
	}
}

Vector2 CalcForce(PMV a, PMV b)
{
	Vector2 con(b.x - a.x, b.y - a.y); // Connection Vector
	TYPE dist = sqrt(pow(b.x - a.x, 2) + pow(b.y - a.y , 2)); // Length
	if (dist > 0.00001)
	{
		return con * ( 1 / dist ) * ( 12 * LJ_E * ( LJ_RM_6 * pow(1 / dist, 7) + LJ_RM_12 * pow(1 / dist , 13)));;
	}
	else
	{
		return -con;
	}
}

void ApplyBoundaries(int i)
{
	if (atoms[i].x > LIMITS_BOX)
	{
		atoms[i].x = LIMITS_BOX;
		atoms[i].V_x *= -1;
		atoms[i].V_y *= 1;
	}
	else if (atoms[i].x < 0)
	{
		atoms[i].x = 0;
		atoms[i].V_x *= -1;
		atoms[i].V_y *= 1;
	}
	if (atoms[i].y > LIMITS_BOX)
	{
		atoms[i].y = LIMITS_BOX;
		atoms[i].V_y *= -1;
		atoms[i].V_x *= 1;
	}
	else if (atoms[i].y < 0)
	{
		atoms[i].y = 0;
		atoms[i].V_y *= -1;
		atoms[i].V_x *= 1;
	}
}

std::vector<int> GetSurroundingCells(int x, int y)
{
	std::vector<int> ret;
	for (int i = -1; i < 2; i++)
	{
		for (int j = -1; j < 2; j++)
		{
			if ( y + j < 0) continue;
			else if (y + j >= LIMITS_BOX / SIZE_CELL) continue;
			else if (x + i < 0) continue;
			else if (x + i >= LIMITS_BOX / SIZE_CELL) continue;
			else
			{
				if (grid[x + i][y + j].size() != 0)
					ret.insert(ret.end(), grid[x + i][y + j].begin(), grid[x + i][y + j].end());
			}
		}
	}
	return ret;
}

void ClearForces()
{
	for (int i = 0; i < NUM_ATOMS; i++)
	{
		forces[i] = Vector2(0, 0);
	}
}

void GeneratePoints()
{
#if RAND == true
	for (int i = 0; i < NUM_ATOMS; i++)
	{
		PMV n(rand() % LIMITS_BOX,
		      rand() % LIMITS_BOX,
		      1 ,
		      MAX_INIT_SPEED * (rand() * 1.0 / RAND_MAX),
		      MAX_INIT_SPEED * (rand() * 1.0 / RAND_MAX));
		atoms[i] = n;
	}
#else
	for (int i = 0, ind = 0; i < NUM_ATOMS; i++, ind += 2)
	{
		PMV n(POINTS[ind] % LIMITS_BOX,
		      POINTS[ind + 1] % LIMITS_BOX,
		      1 ,
		      MAX_INIT_SPEED * (rand() * 1.0 / RAND_MAX),
		      MAX_INIT_SPEED * (rand() * 1.0 / RAND_MAX));
		atoms[i] = n;
	}
#endif
}

void ClearXYZ()
{
	ofstream del("output.xyz", ofstream::out);
	del << "";
	del.close();
}

void WriteToXYZ()
{
	ofstream outFile;
	outFile.open("output.xyz", std::ios_base::app);
	if (outFile.is_open())
	{
		string toWrite = to_string(NUM_ATOMS) + "\n(0,0)AtomConfig\n";
		for (auto atom : atoms) {
			toWrite += "H\t" + to_string(atom.x) + "\t" + to_string(atom.y) + "\t0\n";
		}

		outFile << toWrite << endl;
	}
	outFile.close();
}
