package us.wearecurio.hashids

import java.util.UUID

class DefaultHashIDGenerator extends AbstractHashIDGenerator {

	/**
	 * This class acts as the hashid generator
	 * We are using different unique salts for the hash generation rather
	 * than using different numbers for encoding because
	 * using different unique numbers for different domain instances
	 * will be extra work, also we don't need decoding so using different
	 * numbers for encoding is of no use
	 */
	private static final int DEFAULT_MINIMUM_LENGTH = 6
	private static final long DEFAULT_NUMBER = 11

	// Excluding 0 and i
	private static final String HASHID_CODE_CHARACTERS = "abcdefghjklmnopqrstuvwxyz123456789"

	/**
	 * Used to generate hashid using random UUID as salt
	 * with default minimum length and default number for encryption
	 * @return Hash id
	 */
	String generate() {
		return generate(UUID.randomUUID().toString().replaceAll("-", ""))
	}

	/**
	 * Used to generate hashid using random UUID as salt
	 * with default number for encryption
	 * @param minimumLength minimum length of hashid
	 * @return Hash id
	 */
	String generate(int minimumLength) {
		return generate(UUID.randomUUID().toString().replaceAll("-", ""), minimumLength, DEFAULT_NUMBER)
	}

	/**
	 * Used to generate hashid with default number for encryption
	 * and default minimum length
	 * @param salt custom salt to generate hashid
	 * @return Hash id
	 */
	String generate(String salt) {
		return generate(salt, DEFAULT_MINIMUM_LENGTH, DEFAULT_NUMBER)
	}

	/**
	 * Used to generate hashid with default minimum length
	 * @param salt custom salt to generate hashid
	 * @param numbers numbers to encrypt hashid
	 * @return Hash id
	 */
	String generate(String salt, long... numbers) {
		return generate(salt, DEFAULT_MINIMUM_LENGTH, numbers)
	}

	String getCodeCharacters() {
		return HASHID_CODE_CHARACTERS
	}
}
