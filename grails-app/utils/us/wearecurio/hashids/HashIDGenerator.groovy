package us.wearecurio.hashids

interface HashIDGenerator {

	/**
	 * Used to get sequence of code characters used in the hash id code.
	 * 
	 * @return Code characters
	 * 
	 * @example abcde12345
	 */
	String getCodeCharacters()

	/**
	 * Method which will generates hashid by encode the given numbers based on given salt.
	 * @param salt
	 * @param minimumLength Minimum length of hash id
	 * @param numbers the numbers to encode
	 * @return Encoded hashid
	 */
	String generate(String salt, int minimumLength, long... numbers)

}
