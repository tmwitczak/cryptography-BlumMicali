package cryptography;
import java.security.SecureRandom;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class Key
{
	//-------------------------------------------------------------------------------------------------------- Constants
	public enum KeyLength
	{
		SHORT(1 * 64),
		MEDIUM(2 * 64),
		LONG(3 * 64);

		public final int bits;
		public final int bytes;

		KeyLength(int bits)
		{
			this.bits = bits;
			this.bytes = bits / 8;
		}
	}
	public enum Display
	{
		TEXT,
		BIN,
		HEX
	}

	//----------------------------------------------------------------------------------------------------- Constructors
	public Key(KeyLength length)
	{
		generateRandomKey(length);
	}
	public Key(byte[] bytes) throws Exception
	{
		if (!checkLength(bytes))
			throw new Exception("cryptography.Key must be " + KeyLength.SHORT.bits + ", "
					+ KeyLength.MEDIUM.bits + " or " + KeyLength.LONG.bits + " bits long!");

		this.bytes = bytes.clone();
	}
	public Key(String text, KeyLength length, Display display) throws Exception
	{
		switch(display)
		{
			case TEXT:
				if(2 * text.length() != length.bytes)
					throw new Exception("cryptography.Key must be " + length.bits + " bits long!");

				this.bytes = Converter.stringToBytesUTF8(text);
				break;

			case BIN:
				if(text.length() != length.bytes * 9 - 1)
					throw new Exception("cryptography.Key must be " + length.bits + " bits long!");

				this.bytes = new byte[length.bytes];

				for(int i = 0; i < length.bytes; i++)
					this.bytes[i] = (byte) Integer.parseInt(text.substring(i * 9, i * 9 + 7), 2);

				break;

			case HEX:
				if(text.length() != length.bytes * 3 - 1)
					throw new Exception("cryptography.Key must be " + length.bits + " bits long!");

				this.bytes = new byte[length.bytes];

				for(int i = 0; i < length.bytes; i++)
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
	public KeyLength getKeyLength()
	{
		switch(bytes.length)
		{
			default:
			case 1 * 8: return KeyLength.SHORT;
			case 2 * 8: return KeyLength.MEDIUM;
			case 3 * 8: return KeyLength.LONG;
		}
	}


	//----------------------------------------------------------------------------------------------- Additional methods
	private boolean checkLength(byte[] bytes)
	{
		return bytes.length == KeyLength.SHORT.bytes || bytes.length == KeyLength.MEDIUM.bytes
				|| bytes.length == KeyLength.LONG.bytes;
	}
	private void generateRandomKey(KeyLength length)
	{
		SecureRandom randomNumberGenerator = new SecureRandom();

		this.bytes = new byte[length.bytes];
		randomNumberGenerator.nextBytes(this.bytes);
	}

	//------------------------------------------------------------------------------------------------ Main byte content
	private byte[] bytes;
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*class Key56
		extends Key
{
	// cryptography.Key length constant
	private static final KeyLength KEY_LENGTH = KeyLength.SHORT;

	// Constructors
	public Key56()
	{
		super(KEY_LENGTH);
	}

	public Key56(byte[] bytes) throws Exception
	{
		// Call parent class constructor
		super(bytes);

		// Check key size
		if (bytes.length != KEY_LENGTH.bits)
			throw new Exception("cryptography.Key must be " + KEY_LENGTH.bits + " bits long!");
	}
}*/
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////