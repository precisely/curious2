package us.wearecurio.data

/**
 * A utility class to represent various settings which are simply true or false like whether a user "is active" or
 * not, user should "receive notification" or not. This class is using binary or bit representation as
 * a "long" number instead of using different boolean flags for all. So we can use maximum of 63 configurations
 * (long is 64 bit and the most significant bit is for sign) using one instance of this class. <br><br>
 * 
 * Consider we have 4 settings for a User i.e. "is active", "send notification", "share profile" and "show avatar".
 * Now to represent these 4 settings we can use each bit of a long number starting from right hand side i.e.
 * least significant (LSB).<br>
 * 
 * Suppose a user has marked:<br>
 * <pre>
 * 		"is active" 			true
 * 		"send notification" 	true
 * 		"share profile"			false
 * 		"show avatar"			true
 * </pre>
 * <br>
 * 
 * So we can represent these user settings as: "1011" where the LSB represents the setting "is active". The decimal
 * representation of this above setting will be 11 which we can use to store in a database.<br><br>
 * 
 * Following class uses the term "logical index" which represents the readable position of a bit i.e. position which
 * starts with 1 and the actual "index" which starts with 0.
 * 
 * @see some references
 * http://stackoverflow.com/questions/12015598/how-to-set-unset-a-bit-at-specific-position-of-a-long
 * http://stackoverflow.com/questions/1907318/java-boolean-primitive-type-size
 */
class BitSet {

	long flags = 0

	BitSet() {
		this(0)
	}

	BitSet(long flags) {
		this.flags = flags
	}

	/**
	 * An internal helper method to get the actual index i.e. (logical position/index - 1) and checks if the actual
	 * index falls under 0-62 or logical index falls under 1-63 i.e. only 63 configurations are used.
	 * 
	 * @param logicalIndex Readable position of a bit i.e. position which starts with 1.
	 * @return actual bit index position i.e. logicalIndex - 1
	 */
	private int getBitIndex(int logicalIndex) {
		if (logicalIndex < 1) {
			throw new IndexOutOfBoundsException("Bit position can not be less than 1")
		}

		if (logicalIndex > 63) {
			throw new IndexOutOfBoundsException("Bit index can not be greater than 63")
		}

		return logicalIndex - 1
	}

	/**
	 * Clear the bit for the given logical position to zero. Consider a decimal value 50, binary representation
	 * <pre>00110010</pre>. Calling this method <pre>clear(5)</pre> will reset the bit on position 4th and will become
	 * <pre>00100010</pre> i.e 34 in decimal.
	 * 
	 * @param logicalIndex Readable/logical position of a bit (from right hand side) to clear.
	 */
	void clear(int logicalIndex) {
		flags &= ~(1L << getBitIndex(logicalIndex))
	}

	/**
	 * @param logicalIndex Readable/logical position of a bit (from right hand side) to get the bit value.
	 * @return true if the bit is set to 1 at the specified logical position. So for a decimal value 50 i.e.
	 * <pre>00110010</pre>, calling method <pre>get(5)</pre> will return true and <pre>get(4)</pre> will return false.
	 */
	boolean get(int logicalIndex) {
		return ((flags & (1L << getBitIndex(logicalIndex))) != 0)
	}

	/**
	 * Sets the bit at the given logical index to 1. Consider a decimal value 50, binary representation
	 * <pre>00110010</pre>. Calling this method <pre>set(3)</pre> will set the bit on position 3rd and will become
	 * <pre>00110110</pre> i.e. 54 in decimal.
	 * 
	 * @param logicalIndex Readable/logical position of a bit (from right hand side) to set.
	 */
	void set(int logicalIndex) {
		flags |= (1L << getBitIndex(logicalIndex))
	}
}