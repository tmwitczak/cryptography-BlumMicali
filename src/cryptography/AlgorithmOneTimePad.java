package cryptography;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class AlgorithmOneTimePad
{
	//----------------------------------------------------------------------------------------------------- Main methods
	public byte[] encrypt(byte[] plainText, byte[] key)
	{
		return algorithmBase(plainText, key);
	}
	public byte[] decrypt(byte[] cipherText, byte[] key)
	{
		return algorithmBase(cipherText, key);
	}
	public String encrypt(String plainText, byte[] key)
	{
		return algorithmBase(plainText, key);
	}
	public String decrypt(String cipherText, byte[] key)
	{
		return algorithmBase(cipherText, key);
	}

	//--------------------------------------------------------------------------------------------------- Algorithm base
	byte[] algorithmBase(byte[] text, byte[] key)
	{
		byte[] xorText = new byte[text.length];

		for(int i = 0; i < text.length; i++)
			xorText[i] = (byte)(text[i] ^ key[i]);

		return xorText;
	}
	String algorithmBase(String input, byte[] key)
	{
		byte[] inputBytes = Converter.stringToBytesUTF8(input);
		byte[] outputBytes = algorithmBase(inputBytes, key);

		return Converter.bytesToStringUTF8(outputBytes);
	}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
