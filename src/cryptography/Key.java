package cryptography;

import megaRollo.Matma;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class Key
{
	//-------------------------------------------------------------------------------------------------------- Constants
	public enum Display
	{
		TEXT,
		BIN,
		HEX
	}

	//----------------------------------------------------------------------------------------------------- Constructors
	public Key()
	{
	}
	public Key(byte[] bytes)
	{
		this.bytes = bytes.clone();
	}
	public Key(String text, int numberOfBytes, Display display)
	{
		switch(display)
		{
			case TEXT:
				this.bytes = Converter.stringToBytesUTF8(text);
				break;

			case BIN:
				this.bytes = new byte[numberOfBytes];

				for(int i = 0; i < numberOfBytes - 1; i++)
					this.bytes[i] = (byte) Integer.parseInt(text.substring(i * 8, i * 8 + 7), 2);

				break;

			case HEX:
				this.bytes = new byte[numberOfBytes];

				for(int i = 0; i < numberOfBytes; i++)
					this.bytes[i] = (byte) Integer.parseInt(text.substring(i * 3, i * 3 + 1), 16);

				break;
		}
	}

	//---------------------------------------------------------------------------------------------------------- Getters
	public String getKeyText()
	{
		return Converter.bytesToStringUTF8(bytes);
	}
	public String getKeyHexadecimal()
	{
		StringBuilder keyHexadecimal = new StringBuilder();

		for(int i = 0; i < bytes.length; i++)
			keyHexadecimal.append(String.format("%2s",
					Integer.toHexString(bytes[i] & 0xFF)).replace(' ', '0'));

		return keyHexadecimal.toString().toUpperCase();
	}
	public String getKeyBinary()
	{
		StringBuilder keyBinary = new StringBuilder();

		for(int i = 0; i < bytes.length; i++)
			keyBinary.append(String.format("%8s",
					Integer.toBinaryString(bytes[i] & 0xFF)).replace(' ', '0'));

		return keyBinary.toString();
	}
	public byte[] getBytes()
	{
		return bytes.clone();
	}
	public int getNumberOfBytes()
	{
		return bytes.length;
	}


	//-------------------------------------------------------------------------------------------- Random key generation
	public void generateRandomKey(int numberOfBytes, Matma seed, Matma safePrime)
		throws Exception
	{
		BlumMicaliGenerator blumMicaliGenerator = new BlumMicaliGenerator(seed, safePrime);

		bytes = blumMicaliGenerator.getRandomBytes(numberOfBytes);
	}

	//------------------------------------------------------------------------------------------------ Main byte content
	private byte[] bytes = null;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////