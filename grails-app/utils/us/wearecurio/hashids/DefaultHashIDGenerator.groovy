package us.wearecurio.hashids

import java.util.UUID

class DefaultHashIDGenerator extends AbstractHashIDGenerator {

	private static final int DEFAULT_MINIMUM_LENGTH = 6
	private static final long DEFAULT_NUMBER = 11

	// Excluding 0 and i
	private static final String HASHID_CODE_CHACTERS = "abcdefghjklmnopqrstuvwxyz123456789"

	/**
	 * Used to generate hashid using current epoch time as salt
	 * with default minimum length and default number for encryption
	 * @return Hash id
	 */

	String generate() {
		return generate(UUID.randomUUID().toString().replaceAll("-", ""))
	}

	String generate(int minimumLength) {
		return generate(UUID.randomUUID().toString().replaceAll("-", ""), minimumLength, DEFAULT_NUMBER)
	}

	String generate(String salt) {
		return generate(salt, DEFAULT_MINIMUM_LENGTH, DEFAULT_NUMBER)
	}

	String generate(String salt, long... numbers) {
		return generate(salt, DEFAULT_MINIMUM_LENGTH, numbers)
	}

	String getCodeCharacters() {
		return HASHID_CODE_CHACTERS
	}
}
