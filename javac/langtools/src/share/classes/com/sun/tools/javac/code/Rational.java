package com.sun.tools.javac.code;

/**
 * This clas implemets a rational number consisting of two integers.
 */
public class Rational implements Comparable<Rational>{

	public static final Rational ZERO = new Rational(0);
	public static final Rational ONE = new Rational(1);
	public static final Rational MINUSONE = new Rational(-1);
	public static final Rational MIN_VALUE = new Rational(Integer.MIN_VALUE);
	public static final Rational MAX_VALUE = new Rational(Integer.MAX_VALUE);
	
	public final int numerator;
	public final int denominator;

	public Rational(int numerator) {
		this.numerator = numerator;
		this.denominator = 1;
	}

	public Rational(int numerator, int denominator) {
		if (numerator == 0) {
			this.numerator = 0;
			this.denominator = 1;
		} else {
			if (denominator < 0) {
				numerator = -numerator;
				denominator = -denominator;
			}
			int gcd = gcd(Math.abs(numerator), Math.abs(denominator));
			this.numerator = numerator/gcd;
			this.denominator = denominator/gcd;
		}
	}

	public Rational add(Rational other) {
		return new Rational(numerator*other.denominator + other.numerator*denominator, denominator*other.denominator);
	}
    
    public Rational subtract(Rational other) {
		return new Rational(numerator*other.denominator - other.numerator*denominator, denominator*other.denominator);
	}
    
    public Rational multiply(Rational other) {
		return new Rational(numerator * other.numerator, denominator * other.denominator);
	}
	
	public Rational divide(Rational other) {
		return new Rational(numerator * other.denominator, denominator * other.numerator);
	}
    
    public Rational negate() {
		return new Rational(-this.numerator, this.denominator);
	}
    
    public Rational shorten() {
		int gcd = gcd(numerator, denominator);
		return new Rational(numerator/gcd, denominator/gcd);
	}
    
    public double toDouble() {
		return (double) numerator / (double) denominator;
	}
	
	public int toInt() {
		return numerator / denominator;
	}
	
	public boolean isInt() {
		return denominator == 1;
	}
    
    @Override
	public int compareTo(Rational other) {
		long resultNumerator = ((long)this.numerator)*((long)other.denominator) - ((long)other.numerator)*((long)this.denominator);
		return (resultNumerator == 0) ? 0 : (resultNumerator < 0) ? -1 : 1;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Rational) {
			Rational c = ((Rational) other).shorten();
			Rational shortenedThis = shorten();
			return shortenedThis.numerator == c.numerator && shortenedThis.denominator == c.denominator;
		} else return false;
	}

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.numerator;
        hash = 97 * hash + this.denominator;
        return hash;
    }
    
	@Override
	public String toString() {
		String s = "";
		if (denominator == 1) {
			s += numerator;
		} else {
			s += numerator + "/" + denominator;
		}
		if (numerator < 0 || denominator != 1) {
			s = "(" + s + ")";
		}
		return s;
	}
    
    private static int gcd(int a, int b) {
        // convert tail recursion to loop?
		if (a == 0) {
			return 1;
		} else if (b == 0) {
			return a;
		} else {
			return gcd(b, a%b);
		}
	}
    
}
