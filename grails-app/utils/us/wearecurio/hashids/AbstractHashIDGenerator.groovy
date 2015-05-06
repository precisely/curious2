package us.wearecurio.hashids

abstract class AbstractHashIDGenerator implements HashIDGenerator {

	@Override
	String generate(String salt, int minimumLength, long... numbers) {
		return new Hashids(salt, minimumLength, getCodeCharacters()).encode(numbers)
	}

	@Override
	abstract String getCodeCharacters()

}
