package cryptography;
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class AlgorithmDES
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
	byte[] algorithmBase(byte[] text, Key key, boolean encryption) throws Exception
	{
		Block64Bit[] blocks = divideBytesInto64BitBlocks(text);
		Block32Bit blockLeftHalf = new Block32Bit();
		Block32Bit blockRightHalf = new Block32Bit();
		Block48Bit[] generatedKeys = generateRoundKeys(key);

		for (int i = 0; i < blocks.length; i++)
		{
			initialPermutation(blocks[i]);
			divide64BitBlockIntoHalves(blocks[i], blockLeftHalf, blockRightHalf);

			for(int j = 0; j < numberOfRounds; j++)
				performRound(blockLeftHalf, blockRightHalf, generatedKeys[(encryption ? j : (numberOfRounds - 1 - j))]);

			merge32BitBlockHalvesInto64BitBlock(blockRightHalf, blockLeftHalf, blocks[i]);
			finalPermutation(blocks[i]);
		}

		return merge64BitBlocksIntoBytes(blocks);
	}
	String algorithmBase(String input, Key key, boolean encryption) throws Exception
	{
		byte[] inputBytes = Converter.stringToBytesUTF8(input);
		byte[] outputBytes = algorithmBase(inputBytes, key, encryption);

		return Converter.bytesToStringUTF8(outputBytes);
	}

	//---------------------------------------------------------------------------------------------- Round key generator
	private Block48Bit[] generateRoundKeys(Key key) throws Exception
	{
		Block48Bit[] generatedKeys = new Block48Bit[numberOfRounds];

		Block56Bit key56 = dropParityBits(new Block64Bit(key.getBytes()));

		for (int i = 0; i < numberOfRounds; i++)
		{
			for (int j = 0; j < numberOfKeyShiftsPerRound[i]; j++)
				rotateBitsLeft(key56);

			generatedKeys[i] = compressKey(key56);
		}

		return generatedKeys;
	}

	void rotateBitsLeft(Block56Bit block)
	{
		Block32Bit blockLeft28 = new Block32Bit();
		Block32Bit blockRight28 = new Block32Bit();
		divide56BitBlockIntoHalves(block, blockLeft28, blockRight28);

		moveBitsLeft28(blockLeft28);
		moveBitsLeft28(blockRight28);

		merge28BitBlockHalvesInto56BitBlock(blockLeft28, blockRight28, block);
	}

	//----------------------------------------------------------------------------------------------------- Round method
	private void performRound(Block32Bit leftBlock32, Block32Bit rightBlock32, Block48Bit key)
	{
		Block32Bit copyRightBlock32 = new Block32Bit();
		copyRightBlock32.bytes = rightBlock32.bytes.clone();

		roundFunction(rightBlock32, key);

		for (int i = 0; i < 4; i++)
			leftBlock32.bytes[i] ^= rightBlock32.bytes[i];

		rightBlock32.bytes = leftBlock32.bytes.clone();
		leftBlock32.bytes = copyRightBlock32.bytes.clone();
	}
	private void roundFunction(Block32Bit block32, Block48Bit key48)
	{
		Block48Bit block48 = expandTo48Bits(block32);

		for (int i = 0; i < block48.bytes.length; i++)
			block48.bytes[i] ^= key48.bytes[i];

		Block8Bit[] block6Table = divide48BitBlockInto6BitBlocks(block48);

		Block8Bit[] block4Table = new Block8Bit[8];
		for (int i = 0; i < block4Table.length; i++)
		{
			block4Table[i] = new Block8Bit();
			block4Table[i].bytes[0] = performSubstitution(i, block6Table[i]).bytes[0];
		}

		merge4BitBlocksInto32BitBlock(block4Table, block32);
		
		permutationP(block32);
	}

	//--------------------------------------------------------------------------------------- Block division and merging
	private Block64Bit[] divideBytesInto64BitBlocks(byte[] bytes)
	{
		Block64Bit[] blocks = new Block64Bit[(bytes.length + 8 - 1) / 8];  // one block contains eight bytes

		int bytePosition = 0;
		for(int i = 0; i < blocks.length; i++)
		{
			blocks[i] = new Block64Bit();

			for (int j = 0; j < 8; j++)
				if (bytePosition < bytes.length)
					blocks[i].bytes[j] = bytes[bytePosition++];
				else
					blocks[i].bytes[j] = 0;
		}

		return blocks;
	}
	private byte[] merge64BitBlocksIntoBytes(Block64Bit[] blocks)
	{
		byte[] bytes = new byte[8 * blocks.length];

		for (int i = 0; i < bytes.length; i++)
			bytes[i] = blocks[i / 8].bytes[i % 8];

		return bytes;
	}

	private void divide64BitBlockIntoHalves(Block64Bit block64, Block32Bit block32Left, Block32Bit block32Right)
	{
		for (int i = 0; i < 4; i++)
		{
			block32Left.bytes[i] = block64.bytes[i];
			block32Right.bytes[i] = block64.bytes[i + 4];
		}
	}
	private void merge32BitBlockHalvesInto64BitBlock(Block32Bit blockLeftHalf, Block32Bit blockRightHalf, Block64Bit block)
	{
		for (int i = 0; i < 32; i++)
		{
			block.setBit(i, blockLeftHalf.getBit(i));
			block.setBit(i + 32, blockRightHalf.getBit(i));
		}
	}

	private void divide56BitBlockIntoHalves(Block56Bit block56, Block32Bit blockLeft28, Block32Bit blockRight28)
	{
		for (int i = 0; i < 28; i++)
		{
			blockLeft28.setBit(i, block56.getBit(i));
			blockRight28.setBit(i, block56.getBit(i + 28));
		}
	}
	private void merge28BitBlockHalvesInto56BitBlock(Block32Bit blockLeftHalf, Block32Bit blockRightHalf, Block56Bit block)
	{
		for (int i = 0; i < 28; i++)
		{
			block.setBit(i, blockLeftHalf.getBit(i));
			block.setBit(i + 28, blockRightHalf.getBit(i));
		}
	}

	private Block8Bit[] divide48BitBlockInto6BitBlocks(Block48Bit block)
	{
		Block8Bit[] block6Table = new Block8Bit[8];

		for (int i = 0; i < 48; i++)
		{
			block6Table[i / 6] = new Block8Bit();
			block6Table[i / 6].setBit(i % 6, block.getBit(i));
		}

		return block6Table;
	}
	private void merge4BitBlocksInto32BitBlock(Block8Bit[] block4Table, Block32Bit block)
	{
		for (int i = 0; i < block.bytes.length * Block64Bit.numberOfBitsInByte; i++)
			block.setBit(i, block4Table[i / 4].getBit(i % 4));
	}

	//----------------------------------------------------------------------------------------------------- Permutations
	private void initialPermutation(Block64Bit block)
	{
		Block64Bit temp = new Block64Bit();

		for (int i = 0; i < permutationInitialTable.length; i++)
			temp.setBit(i, block.getBit(permutationInitialTable[i] - 1));

		block.bytes = temp.bytes.clone();
	}
	private void finalPermutation(Block64Bit block)
	{
		Block64Bit output = new Block64Bit();



		for (int i = 0; i < 64; i++)
			output.setBit(i, block.getBit(permutationFinalTable[i] - 1));

		block.bytes = output.bytes.clone();
	}

	private Block56Bit dropParityBits(Block64Bit block64)
	{
		Block56Bit block56 = new Block56Bit();

		for (int i = 0; i < permutationParityDropTable.length; i++)
			block56.setBit(i, block64.getBit(permutationParityDropTable[i] - 1));

		return block56;
	}
	private Block48Bit compressKey(Block56Bit block)
	{
		Block48Bit block48 = new Block48Bit();

		for (int i = 0; i < 48; i++)
			block48.setBit(i, block.getBit(permutationTableKeyCompression[i] - 1));

		return block48;
	}
	private Block48Bit expandTo48Bits(Block32Bit block32)
	{
		Block48Bit block48 = new Block48Bit();

		for (int i = 0; i < 48; i++)
			block48.setBit(i, block32.getBit(permutationExpansionTable[i] - 1));

		return block48;
	}
	private void moveBitsLeft28(Block32Bit block28)
	{
		boolean orphanBit = block28.getBit(0);

		for (int i = 0; i < 27; i++)
			block28.setBit(i, block28.getBit(i + 1));

		block28.setBit(27, orphanBit);
	}

	private Block8Bit performSubstitution(int n, Block8Bit input)
	{
		Block8Bit output = new Block8Bit();

		int[] inputBits = new int[6];
		for(int i = 0; i < inputBits.length; i++)
			inputBits[i] = (input.getBit(i) ? 1 : 0);

		int rowNumber = 2 * inputBits[0] + inputBits[5];
		int columnNumber = 8 * inputBits[1] + 4 * inputBits[2] + 2 * inputBits[3] + inputBits[4];

		output.bytes[0] = (byte)(substitutionBoxesTable[n][rowNumber * 16 + columnNumber] << 4);

		return output;
	}

	private Block32Bit permutationP(Block32Bit block32)
	{
		Block32Bit output = new Block32Bit();

		final byte[] permutationPBlockTable = {
				16, 7, 20, 21,
				29, 12, 28, 17,
				1, 15, 23, 26,
				5, 18, 31, 10,
				2, 8, 24, 14,
				32, 27, 3, 9,
				19, 13, 30, 6,
				22, 11, 4, 25
		};

		for (int i = 0; i < 32; i++)
			output.setBit(i, block32.getBit(permutationPBlockTable[i] - 1));

		for(int i = 0; i < 32; i++)
			block32.setBit(i, output.getBit(i));

		return output;
	}

	//-------------------------------------------------------------------------------------------------------- Constants
	static final int numberOfRounds = 16;
	static final int[] numberOfKeyShiftsPerRound = {1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1};
	static final int[] permutationInitialTable = {
			58, 50, 42, 34, 26, 18, 10, 2,
			60, 52, 44, 36, 28, 20, 12, 4,
			62, 54, 46, 38, 30, 22, 14, 6,
			64, 56, 48, 40, 32, 24, 16, 8,
			57, 49, 41, 33, 25, 17, 9, 1,
			59, 51, 43, 35, 27, 19, 11, 3,
			61, 53, 45, 37, 29, 21, 13, 5,
			63, 55, 47, 39, 31, 23, 15, 7
	};
	static final int[] permutationFinalTable =
			{
					40, 8, 48, 16, 56, 24, 64, 32,
					39, 7, 47, 15, 55, 23, 63, 31,
					38, 6, 46, 14, 54, 22, 62, 30,
					37, 5, 45, 13, 53, 21, 61, 29,
					36, 4, 44, 12, 52, 20, 60, 28,
					35, 3, 43, 11, 51, 19, 59, 27,
					34, 2, 42, 10, 50, 18, 58, 26,
					33, 1, 41, 9, 49, 17, 57, 25
			};
	static final int[] permutationParityDropTable =
			{
					57,	49,	41,	33,	25,	17,	9,	1,
					58,	50,	42,	34,	26,	18,	10,	2,
					59,	51,	43,	35,	27,	19,	11,	3,
					60,	52,	44,	36,	63,	55,	47,	39,
					31,	23,	15,	7,	62,	54,	46,	38,
					30,	22,	14,	6,	61,	53,	45,	37,
					29,	21,	13,	5,	28,	20,	12,	4
			};
	static final int[] permutationTableKeyCompression =
			{
					14,	17,	11,	24,	1,	5,	3,	28,
					15,	6,	21,	10,	23,	19,	12,	4,
					26,	8,	16,	7,	27,	20,	13,	2,
					41,	52,	31,	37,	47,	55,	30,	40,
					51,	45,	33,	48,	44,	49,	39,	56,
					34,	53,	46,	42,	50,	36,	29,	32
			};
	static final byte[][] substitutionBoxesTable = {
			{
					14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
					0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
					4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
					15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13
			},
			{
					15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
					3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5,
					0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
					13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9
			},
			{
					10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
					13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
					13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
					1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12
			},
			{
					7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
					13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
					10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
					3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14
			},
			{
					2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
					14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
					4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
					11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3
			},
			{
					12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
					10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
					9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
					4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13
			},
			{
					4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
					13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
					1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
					6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12
			},
			{
					13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
					1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
					7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
					2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11
			}
	};
	final int[] permutationExpansionTable = {
			32, 1, 2, 3, 4, 5,
			4, 5, 6, 7, 8, 9,
			8, 9, 10, 11, 12, 13,
			12, 13, 14, 15, 16, 17,
			16, 17, 18, 19, 20, 21,
			20, 21, 22, 23, 24, 25,
			24, 25, 26, 27, 28, 29,
			28, 29, 30, 31, 32, 1
	};
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
class Block64Bit
{
	//----------------------------------------------------------------------------------------------------- Constructors
	public Block64Bit()
	{
		bytes = new byte[8];
	}
	public Block64Bit(byte[] bytes) throws Exception
	{
		if(bytes.length != 8)
			throw new Exception("Bytes in Block64Bit constructor must be 8 bytes long!");

		this.bytes = bytes.clone();
	}
	Block64Bit(int numberOfBytes)
	{
		bytes = new byte[numberOfBytes];
	}

	//----------------------------------------------------------------------------------------------- Bit access methods
	// Positions numbered beginning with the most significant bit
	public boolean getBit(int position)
	{
		return (bytes[position / numberOfBitsInByte] & (1 << (numberOfBitsInByte - 1 - (position % numberOfBitsInByte)))) != 0;
	}

	public void setBit(int position, boolean bitValue)
	{
		if (bitValue)
			bytes[position / numberOfBitsInByte] |= (1 << (numberOfBitsInByte - 1 - (position % numberOfBitsInByte)));
		else
			bytes[position / numberOfBitsInByte] &= ~(1 << (numberOfBitsInByte - 1 - (position % numberOfBitsInByte)));
	}

	//------------------------------------------------------------------------------------------------ Main byte content
	byte[] bytes;
	
	//-------------------------------------------------------------------------------------------------------- Constants
	static final int numberOfBitsInByte = 8;
}

//--------------------------------------------------------------------------------------------------------------------//
class Block32Bit
		extends Block64Bit
{
	Block32Bit()
	{
		super(4);
	}
}

//--------------------------------------------------------------------------------------------------------------------//
class Block48Bit
		extends Block64Bit
{
	Block48Bit()
	{
		super(6);
	}
}

//--------------------------------------------------------------------------------------------------------------------//
class Block56Bit
		extends Block64Bit
{
	Block56Bit()
	{
		super(7);
	}
}

//--------------------------------------------------------------------------------------------------------------------//
class Block8Bit
		extends Block64Bit
{
	Block8Bit()
	{
		super(1);
	}
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////