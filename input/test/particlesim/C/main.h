#include <stdio.h>
#include <stdlib.h>
#include <vector>
#include <math.h>
#include <fstream>
#include "Defines.h"

struct PMV
{
    TYPE x;
    TYPE y;
    TYPE m;
    TYPE V_x;
    TYPE V_y;
    PMV(): x(0), y(0), m(1), V_x(0), V_y(0) {};
    PMV(TYPE x, TYPE y, TYPE m, TYPE V_x, TYPE V_y): x(x), y(y), m(m), V_x(V_x), V_y(V_y) {};
    PMV &operator +=(const PMV &b)
    {
        x += b.x; y += b.y;
        return *this;
    }
};
struct Vector2
{
    TYPE x;
    TYPE y;
    Vector2(TYPE x = 0, TYPE y = 0): x(x), y(y) {}
    Vector2 operator +(const Vector2 &a) const
    {
        return Vector2(a.x + x, a.y + y);
    }
    Vector2 operator -() const
    {
        return Vector2(-x, -y);
    }
    Vector2 operator -(const Vector2 &b) const
    {
        return Vector2(b.x - x, b.y - y);
    }
    Vector2 &operator +=(const Vector2 &b)
    {
        x += b.x; y += b.y;
        return *this;
    }
    Vector2 operator *(const TYPE b) const
    {
        return Vector2(x * b, y * b);
    }
    Vector2 operator /(const TYPE b) const
    {
        return Vector2(x / b, y / b);
    }
};

void InitializeForces();
void GeneratePoints();

void ClearForces();
void NonBondedForces();
void AccumForces();

void Interpolate();
void ApplyBoundaries(int i);

Vector2 CalcForce(PMV a, PMV b);
#if GRID == true
std::vector<int> GetSurroundingCells(int x, int y);
void UpdateGrid();
#endif

void WriteToXYZ();
void ClearXYZ();
