package cryptography;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import megaRollo.Matma;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class BlumMicaliGenerator
{
	//--------------------------------------------------------------------------------------------- Generator parameters
	private Matma seed;
	private Matma safePrime;
	private Matma primitiveRoot;
	private Matma generatedNumber;

	//------------------------------------------------------------------------------------------------------ Constructor
	public BlumMicaliGenerator(Matma seed, Matma safePrime)
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

		generatedNumber = seed.add(Matma.ZERO);        // make a copy of seed

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

		if (safePrime.subtract(Matma.ONE).divide(Matma.TWO).compareTo(generatedNumber) >= 0)
			return true;
		else
			return false;
	}

	//------------------------------------------------------------------------------------------------- Primality checks
	private boolean isNumberSafePrime(Matma number)
	{
		return isNumberPrime(number) && isNumberPrime(number.subtract(Matma.ONE).divide(Matma.TWO));
	}
	private boolean isNumberPrime(Matma number)
	{
		return number.isProbablePrime(64);
	}

	//-------------------------------------------------------------------------------------------------- Primitive roots
	private Matma getPrimitiveRootOfSafePrime(Matma safePrime)
		throws Exception
	{
		switch(safePrime.mod(new Matma("8")).intValue())
		{
			case 3:     return Matma.TWO;
			case 7:     return safePrime.subtract(Matma.TWO);
			default:    throw new Exception("Primitive root can't be found for that safe prime!");
		}
	}
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////