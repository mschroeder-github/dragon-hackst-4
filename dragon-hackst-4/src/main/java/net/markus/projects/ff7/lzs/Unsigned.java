package net.markus.projects.ff7.lzs;

/**
* Represents a Unsigned Data Type.
* Only Byte, Short or Int are possible types.
*/
public class Unsigned {

	//ATTRIBUTES
	
	/**
	* The inner type of the Unsigned Object
	*/
	private Type type;
	
	/**
	* The value
	*/
	private long value;

	/**
	* The bit representation
	*/
	private boolean[] bits;




	//CONSTRUCTORS

	/**
	* Constructs a Unsigned Object.
	* @param t the type { BYTE, SHORT, INT }
	*/
	public Unsigned(Type t) {
		type = t;
		bits = new boolean[getSize()];
	}	
	/**
	* Constructs a Unsigned Object.
	* @param t the type { BYTE, SHORT, INT }
	* @param value the initial value. If the value is smaller/bigger than minimal/maximal value, it will set automaticly to minimal/maximal value.
	*/
	public Unsigned(Type t, long value) {
		this(t);
		setValue(value);	
	}
	/**
	* Constructs a Unsigned Object.
	* @param t the type { BYTE, SHORT, INT }
	* @param value the initial value in bit form. If the value is smaller/bigger than minimal/maximal value, it will set automaticly to minimal/maximal value.
	*/
	public Unsigned(Type t, boolean[] value) {
		this(t);
		setBitValue(value);	
	}
	/**
	* Constructs a Unsigned Object.
	* @param t the type { BYTE, SHORT, INT }
	* @param value the initial value in byte form. If the value is smaller/bigger than minimal/maximal value, it will set automaticly to minimal/maximal value.
	*/
	public Unsigned(Type t, int[] value) {
		this(t);
		setByteValue(value);
	}
	/**
	* Constructs a Unsigned Object.
	* @param t the type { BYTE, SHORT, INT }
	* @param value the initial value in String form
	*/
	public Unsigned(Type t, String value) {
		this(t, Long.parseLong(value));
	}


	// CONVERTERS

	/**
	* @return a new object with same value but new type
	*/
	public Unsigned convert(Type t) {
		return new Unsigned(t, this.value);
	}

	
	//EXTRACTORS
	
	/**
	* Extracts on bit level the bits and returns the extracted unsigned object.
	* [start, end]
	* @param start the start index of the bit string.
	* @param end the end index of the bit string.
	*/
	public Unsigned extract(int start, int end) {
		
		boolean[] newBits = new boolean[end - start + 1];

		for(int i = start; i <= end; i++) {
			newBits[i - start] = bits[i];
		}

		return new Unsigned(this.type, newBits);
	}


	//ATTRIBUT SET SECTION
	
	/**
	* Sets the value.
	* If the value is smaller/bigger than minimal/maximal value, it will set automaticly to minimal/maximal value. 
	*/
	public void setValue(long value) {
		if(value < getMinValue())
			value = getMinValue();
		if(value > getMaxValue())
			value = getMaxValue();
		this.value = value;

	 	valueToBits();
	}
	/**
	* Sets the value by boolean array representing bits.
	* If the value is smaller/bigger than minimal/maximal value, it will set automaticly to minimal/maximal value. 
	*/
	public void setBitValue(boolean[] value) {
		long newValue = 0;
		for(int i = 0; i < Math.min(value.length, getSize()); i++)
			newValue += value[i] ? (long) Math.pow(2, i) : 0;

		setValue(newValue);
	}
	
	/**
	* Sets the value by int array representing bytes.
	* If the value is smaller/bigger than minimal/maximal value, it will set automaticly to minimal/maximal value. 
	*/
	public void setByteValue(int[] value) {
		long newValue = 0;
		for(int i = 0; i < Math.min(value.length, getSize() / 8); i++)
			newValue += Math.pow(256, i) * value[i];

		setValue(newValue);
	}

	/**
	* Converts the value attribute in bits boolean array
	*/
	private void valueToBits() {
		for(int i = getSize() - 1; i >= 0; i--) {
			
			long onebit = value & (long) Math.pow(2, i);
			onebit = onebit >>> i;
			
			// bits[0] = 1. bit, ..., bits[7] = 8. bit
			bits[i] = (onebit == 1) ? true : false;
		}
	}


	//CALCULATED GET SECTION

	/**
	* @param part seperate partlength bits in parts.
	* @return String representation of the bits.
	* example: 00000011 = 3
	*/
	public String getBitString(int partlength) {
		StringBuffer sb = new StringBuffer();
		for(int i = bits.length - 1; i >= 0; i--) {
			sb.append(bits[i] ? '1' : '0');
			
			if(i != 0 && i % partlength == 0) 
				sb.append(' ');
		}
		return sb.toString();
	}
	/** 
	* @return String representation of the bits seperated in 8bit parts.
	*/
	public String getBitString() {
		return getBitString(8);
	}


	/**
	* @return the fixed point calculation with n 
	*/
	public double getFixedPoint(int n) {
		int k = getSize();
		int m = k - n;
		
		double value = 0;

		//n = 4
		//0000 000000000000... <-

		//befor point
		int p = 0;
		for(int i = m; i < k; i++) {
			value += bits[i] ? Math.pow(2, p) : 0;
			p++;
		}

		//after point
		p = 1;
		for(int i = m-1; i >= 0; i--) {
			value += bits[i] ? Math.pow(2, -p) : 0;
			p++;
		}

		return value;
	}

	/**
	* @return a String representatin of the unsigned object
	*/
	public String toString() {
		return "" + getType() + " " + getBitString() + " " + getValue();
	}


	//ATTRIBUT GET SECTION

	/**
	* @return the type
	*/
	public Type getType() { return type; }
	
	/**
	* @return the value
	*/
	public long getValue() { return value; }

	/**
	* @return bit boolean string
	*/
	public boolean[] getBits() {
		return bits;
	}

	/**
	* @return byte array
	*/
	public int[] getBytes() {
		int[] bytes = new int[getSize()/8];
		for(int j = 0; j < bytes.length; j++) {
			for(int i = 0; i < 8; i++) {
				bytes[j] += (int) Math.pow(2, i) * (bits[8 * j + i] ? 1 : 0);
			}
		}
		return bytes;
	}

	/**
	* @return one bit (in boolean) by index
	*/
	public boolean getBit(int index) {
		return bits[index];
	}

	/**
	* @return the most significant bit (MSB)
	*/
	public boolean getMSB() {
		return getBit(bits.length - 1);
	}

	/**
	* @return the last significant bit (LSB)
	*/
	public boolean getLSB() {
		return getBit(0);
	}

	//STATIC GET SECTION

	/**
	* @return the minimal value
	*/
	public long getMinValue() {
		return 0;
	}
	/**
	* @return the maximal value
	*/
	public long getMaxValue() {
		switch(type) {
			case BYTE: return 255L;
			case SHORT: return 65535L;
			case INT: return 4294967295L;
			default: return 0;
		}
	}
	/**
	* @return the number of bits used to represent the value in unsigned binary form
	*/
	public int getSize() {
		switch(type) {
			case BYTE: return 8;
			case SHORT: return 16;
			case INT: return 32;
			default: return 0;
		}
	}


	//STATIC METHODS
	
	/**
	* Returns null if the first and second Unsigned are not bytes.
	*/
	public static Unsigned toShort(Unsigned firstByte, Unsigned secondByte) {
		if(firstByte.getType() != Type.BYTE || secondByte.getType() != Type.BYTE)
			return null;

		boolean[] fullbits = new boolean[16];

		System.arraycopy(firstByte.getBits(), 0, fullbits, 0, firstByte.getBits().length);
		System.arraycopy(secondByte.getBits(), 0, fullbits, firstByte.getBits().length, secondByte.getBits().length);
	
		return new Unsigned(Type.SHORT, fullbits);
	}
}

