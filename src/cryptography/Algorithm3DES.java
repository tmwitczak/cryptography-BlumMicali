package cryptography;
import java.util.Arrays;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class Algorithm3DES
{
	//----------------------------------------------------------------------------------------------------- Main methods
	public byte[] encrypt(byte[] plainText, Key key) throws Exception
	{
		return algorithmBase(plainText, key, true);
	}
	public byte[] decrypt(byte[] cipherText, Key key) throws Exception
	{
		return algorithmBase(cipherText, key, false);
	}
	public String encrypt(String plainText, Key key) throws Exception
	{
		return algorithmBase(plainText, key, true);
	}
	public String decrypt(String cipherText, Key key) throws Exception
	{
		return algorithmBase(cipherText, key, false);
	}

	//--------------------------------------------------------------------------------------------------- Algorithm base
	private byte[] algorithmBase(byte[] input, Key key, boolean encryption) throws Exception
	{
		Key[] keys = new Key[3];
		byte[] output;

		switch(key.getKeyLength())
		{
		case SHORT:
			keys[0] = new Key(key.getBytes());
			keys[1] = new Key(keys[0].getBytes().clone());
			keys[2] = new Key(keys[0].getBytes().clone());
			break;

		case MEDIUM:
			keys[0] = new Key(Arrays.copyOfRange(key.getBytes(), 0, 8));
			keys[1] = new Key(Arrays.copyOfRange(key.getBytes(), 8, 16));
			keys[2] = new Key(keys[0].getBytes().clone());
			break;

		case LONG:
			keys[0] = new Key(Arrays.copyOfRange(key.getBytes(), 0, 8));
			keys[1] = new Key(Arrays.copyOfRange(key.getBytes(), 8, 16));
			keys[2] = new Key(Arrays.copyOfRange(key.getBytes(), 16, 24));
			break;
		}

		output = algorithmDES.algorithmBase(input, keys[0], encryption);
		output = algorithmDES.algorithmBase(output, keys[1], !encryption);
		output = algorithmDES.algorithmBase(output, keys[2], encryption);

		return output;
	}
	private String algorithmBase(String input, Key key, boolean encryption) throws Exception
	{
		byte[] inputBytes = Converter.stringToBytesUTF8(input);
		byte[] outputBytes = algorithmBase(inputBytes, key, encryption);

		return Converter.bytesToStringUTF8(outputBytes);
	}

	//---------------------------------------------------------------------------------------------------- DES encryptor
	private static final AlgorithmDES algorithmDES = new AlgorithmDES();
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////