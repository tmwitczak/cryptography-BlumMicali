package cryptography;
import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
public class KeyFormatter
{
	public static void setFormatter(Key.KeyLength length, Key.Display display, JFormattedTextField textField)
	{
		try
		{
			MaskFormatter maskFormatter = new MaskFormatter();

			StringBuilder mask = new StringBuilder();
			String maskPattern = null;

			switch (display)
			{
				case TEXT:
					maskPattern = "*";
					break;
				case BIN:
					maskPattern = "********";
					maskFormatter.setValidCharacters("01");
					break;
				case HEX:
					maskPattern = "**";
					maskFormatter.setValidCharacters("0123456789abcdefABCDEF");
					break;
			}

			for (int i = 0; i < length.bytes; i++)
			{
				if(display.equals(Key.Display.TEXT))
				{
					if (i < (length.bytes / 2))
						mask.append(maskPattern);
				}
				else
				{
					mask.append(maskPattern);
				}

				if (!display.equals(Key.Display.TEXT) && i < length.bytes - 1)
					mask.append(' ');
			}


			maskFormatter.setMask(mask.toString());
			maskFormatter.setPlaceholderCharacter('\u02FD');

			textField.setText("");
			textField.setFormatterFactory(new DefaultFormatterFactory(maskFormatter));

		}
		catch (Exception exception)
		{
			exception.printStackTrace();
		}
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////