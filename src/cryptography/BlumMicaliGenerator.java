package cryptography;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import java.math.BigInteger;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class BlumMicaliGenerator
{
	//--------------------------------------------------------------------------------------------- Generator parameters
	private BigInteger seed;
	private BigInteger safePrime;
	private BigInteger primitiveRoot;
	private BigInteger generatedNumber;

	//------------------------------------------------------------------------------------------------------ Constructor
	public BlumMicaliGenerator(BigInteger seed, BigInteger safePrime)
		throws Exception
	{
		this.seed = seed;                                                   // x[0]

		if(!isNumberSafePrime(this.safePrime = safePrime))                  // p
			throw new Exception("Number is not a safe prime!");

		this.primitiveRoot = getPrimitiveRootOfSafePrime(this.safePrime);   // g
	}

	//---------------------------------------------------------------------------------------------- Randomizing methods
	public byte[] getRandomBytes(int numberOfBytes)
	{
		byte[] bytes = new byte[numberOfBytes];

		generatedNumber = seed.add(BigInteger.ZERO);        // make a copy of seed

		for(int i = 0; i < bytes.length; i++)
			bytes[i] = getNextRandomByte();

		return bytes;
	}

	private byte getNextRandomByte()
	{
		byte b = 0;

		for (int i = 0; i < 8; i++)
			b += (getNextRandomBit() ? 1 : 0) << i;

		return b;
	}

	private boolean getNextRandomBit()
	{
		generatedNumber = primitiveRoot.modPow(generatedNumber, safePrime);      // x[i+1] = g^x[i] mod p

		if (safePrime.subtract(BigInteger.ONE).divide(BigInteger.TWO).compareTo(generatedNumber) >= 0)
			return true;
		else
			return false;
	}

	//------------------------------------------------------------------------------------------------- Primality checks
	private boolean isNumberSafePrime(BigInteger number)
	{
		return isNumberPrime(number) && isNumberPrime(number.subtract(BigInteger.ONE).divide(BigInteger.TWO));
	}
	private boolean isNumberPrime(BigInteger number)
	{
		return number.isProbablePrime(64);
	}

	//-------------------------------------------------------------------------------------------------- Primitive roots
	private BigInteger getPrimitiveRootOfSafePrime(BigInteger safePrime)
		throws Exception
	{
		switch(safePrime.mod(new BigInteger("8")).intValue())
		{
			case 3:     return BigInteger.TWO;
			case 7:     return safePrime.subtract(BigInteger.TWO);
			default:    throw new Exception("Primitive root can't be found for that safe prime!");
		}
	}
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////