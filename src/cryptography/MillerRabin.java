///**
// ** Java Program to Implement Miller Rabin Primality Test Algorithm
// **/
//package cryptography;
//
//import java.math.BigInteger;
//import java.util.Random;
//import java.util.Scanner;
//
///** Class MillerRabin **/
//public class MillerRabin
//{
//	/** Function to check if prime or not **/
//	public boolean isPrime(BigInteger n, int iteration)
//	{
//		/** base case **/
//		if (n == 0 || n == 1)
//			return false;
//		/** base case - 2 is prime **/
//		if (n == 2)
//			return true;
//		/** an even number other than 2 is composite **/
//		if (n % 2 == 0)
//			return false;
//
//		BigInteger s = n - 1;
//		while (s % 2 == 0)
//			s /= 2;
//
//		Random rand = new Random();
//		for (int i = 0; i < iteration; i++)
//		{
//			BigInteger r = Math.abs(rand.nextBigInteger());
//			BigInteger a = r % (n - 1) + 1, temp = s;
//			BigInteger mod = modPow(a, temp, n);
//			while (temp != n - 1 && mod != 1 && mod != n - 1)
//			{
//				mod = mulMod(mod, mod, n);
//				temp *= 2;
//			}
//			if (mod != n - 1 && temp % 2 == 0)
//				return false;
//		}
//		return true;
//	}
//	/** Function to calculate (a ^ b) % c **/
//	public BigInteger modPow(BigInteger a, BigInteger b, BigInteger c)
//	{
//		BigInteger res = 1;
//		for (int i = 0; i < b; i++)
//		{
//			res *= a;
//			res %= c;
//		}
//		return res % c;
//	}
//	/** Function to calculate (a * b) % c **/
//	public BigInteger mulMod(BigInteger a, BigInteger b, BigInteger mod)
//	{
//		return BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).mod(BigInteger.valueOf(mod)).BigIntegerValue();
//	}
//	/** Main function **/
//	public static void main (String[] args)
//	{
//		Scanner scan = new Scanner(System.in);
//		System.out.println("Miller Rabin Primality Algorithm Test\n");
//		/** Make an object of MillerRabin class **/
//		MillerRabin mr = new MillerRabin();
//		/** Accept number **/
//		System.out.println("Enter number\n");
//		BigInteger num = scan.nextBigInteger();
//		/** Accept number of iterations **/
//		System.out.println("\nEnter number of iterations");
//		int k = scan.nextInt();
//		/** check if prime **/
//		boolean prime = mr.isPrime(num, k);
//		if (prime)
//			System.out.println("\n"+ num +" jest pierwsza ");
//		else
//			System.out.println("\n"+ num +" nie jest liczba pierwsza");
//
//	}
//}
