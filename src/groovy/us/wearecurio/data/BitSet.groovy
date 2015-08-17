package us.wearecurio.data

/**
 * A utility class to represent various settings to store them as a binary representation instead of using a boolean
 * field for all. We are using a long number to reprensent it as a binary representation so we can use 64 configuration
 * using an instance of this class. <br><br>
 * 
 * Consider we have 4 settings for a User i.e. "is active", "send notification", "share profile" and "show avatar".
 * Now to represent these 4 settings we can use each bit of a binary number starting from left hand side i.e.
 * least significant bit of a binary number.<br>
 * 
 * Suppose a user has marked:<br>
 * 		"is active" 			true
 * 		"send notification" 	false
 * 		"share profile"			false
 * 		"show avatar"			true
 * 
 * So we can represent this user settings as: "0 0 0 0 0 0 1 0 0 1" where the least significant bit represents the
 * setting "is active". The decimal representation of this above setting will be 9. In this way, we can use a single
 * long number to represent 64 configurations. 
 */
class BitSet {

	/**
	 * Internal representation of binary number as an array representing each position as a each bit where the 0th
	 * position contains the least significant bit and last position contains the most significant bit. So, a binary
	 * representation of a decimal number 50 is "00110010" and will be stored here as [0, 1, 0, 0, 1, 1, 0, 0] i.e.
	 * in the reverse order for easy calculation.
	 * 
	 * Marking this field as transient so that, Grails and Hibernate do not create a mapping for this.
	 */
	transient protected long[] bits = new long[64]
	
	/**
	 * Decimal representation of the 64 bit binary representation.
	 */
	private Long flags

	BitSet() {
		this(0)
	}

	BitSet(Long flags) {
		setFlags(flags)
	}

	/**
	 * Clear the bit for the given index or position to zero. Consider a decimal value 50 and its stored binary value
	 * [0, 1, 0, 0, 1, 1, 0, 0]. Calling this method <pre>clear(4)</pre> will reset the bit on position 4th and will
	 * become [0, 1, 0, 0, 0, 1, 0, 0] i.e. 34
	 */
	void clear(int position) {
		bits[position] = 0
	}

	/**
	 * Return true if the bit is set to 1 at the specified position.
	 */
	boolean get(int position) {
		return bits[position]
	}

	/**
	 * Getter method to return the decimal value of all the bits.
	 */
	Long getFlags() {
		return Long.parseLong(bits.toList().reverse().join(""), 2)
	}

	/**
	 * Sets the bit at the specified index to 1. Consider a decimal value 50 and its stored binary value
	 * [0, 1, 0, 0, 1, 1, 0, 0]. Calling this method <pre>set(3)</pre> will set the bit on position 3rd and will
	 * become [0, 1, 0, 1, 1, 1, 0, 0] i.e. 58
	 * @param position
	 */
	void set(int position) {
		bits[position] = 1
	}

	/**
	 * 
	 * @param flags
	 */
	void setFlags(Long flags) {
		this.flags = flags

		/*
		 * Convert the given long value as decimal string and store its binary representation as a list. For example:
		 * Binrary representation of number 2 is "10" and will be stored as: [0, 1]
		 */
		Long.toBinaryString(flags).split("(?!^)").reverse()*.toInteger().eachWithIndex { bit, index ->
			this.bits[index] = bit
		}
	}
}