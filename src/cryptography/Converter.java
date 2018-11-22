package cryptography;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Converter
{
	//----------------------------------------------------------------------------------------------------- UTF8 / bytes
	static byte[] stringToBytesUTF8(String string)
	{
		byte[] bytes = new byte[string.length() << 1];
		int position = 0;

		for(char character : string.toCharArray())
		{
			bytes[position++] = (byte) ((character & 0xFF00) >> 8);
			bytes[position++] = (byte) (character & 0x00FF);
		}

		return bytes;
	}
	static String bytesToStringUTF8(byte[] bytes)
	{
		char[] buffer = new char[bytes.length >> 1];

		for(int i = 0; i < buffer.length; i++)
		{
			int bpos = i << 1;
			char c = (char)(((bytes[bpos]&0x00FF)<<8) + (bytes[bpos+1]&0x00FF));
			buffer[i] = c;
		}
		return new String(buffer);
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////