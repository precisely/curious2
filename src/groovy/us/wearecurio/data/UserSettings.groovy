package us.wearecurio.data

class UserSettings extends BitSet {

	private static final int PRIVACY_BIO_POSITION = 0
	private static final int PRIVACY_NAME_POSITION = 1

	static transients = ["bioPublic", "namePublic", "value"]

	UserSettings() {
		super(0)
	}

	UserSettings(Long value) {
		super(value)
	}

	/**
	 * Get the decimal representation of all the applied settings.
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