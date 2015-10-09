package us.wearecurio.data

class UserSettings extends BitSet {

	/*
	 * Define logical positions of all user related settings here starting with 1 and upto 64.
	 */
	private static final int PRIVACY_BIO_POSITION = 1
	private static final int PRIVACY_NAME_POSITION = 2

	// Do not save following fields into the database
	static transients = ["bioPublic", "namePublic", "value"]

	UserSettings() {
		super(0)
	}

	UserSettings(Long value) {
		super(value)
	}

	/**
	 * An alias method to get the decimal representation of all the settings which internally calls
	 * {@link us.wearecurio.data.BitSet#getFlags getFlags()}.
	 * 
	 * @return Decimal value of all settings.
	 */
	Long getValue() {
		return this.getFlags()
	}

	boolean isBioPublic() {
		return get(PRIVACY_BIO_POSITION)
	}

	boolean isNamePublic() {
		return get(PRIVACY_NAME_POSITION)
	}

	void makeBioPrivate() {
		clear(PRIVACY_BIO_POSITION)
	}

	void makeBioPublic() {
		set(PRIVACY_BIO_POSITION)
	}

	void makeNamePrivate() {
		clear(PRIVACY_NAME_POSITION)
	}

	void makeNamePublic() {
		set(PRIVACY_NAME_POSITION)
	}
}