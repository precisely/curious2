package us.wearecurio.data

import us.wearecurio.model.ThirdParty

class UserSettings extends BitSet {

	/*
	 * Define logical positions of all user related settings here starting with 1 and upto 64.
	 */
	private static final int PRIVACY_BIO_POSITION = 1
	private static final int PRIVACY_NAME_POSITION = 2
	private static final int CLOSED_EXPLANATION_CARD_CURIOSITIES = 3
	private static final int CLOSED_EXPLANATION_CARD_TRACKATHON = 4
	private static final int TRACKATHON_VISITED = 5

	private static final int WITHINGS_COLLAPSED = 6
	private static final int FITBIT_COLLAPSED = 7
	private static final int MOVES_COLLAPSED = 8
	private static final int JAWBONE_COLLAPSED = 9
	private static final int OURA_COLLAPSED = 10

	// Do not save following fields into the database
	static transients = ["bioPublic", "namePublic", "value", "deviceSettings"]

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

	void closeCuriositiesExplanation() {
		set(CLOSED_EXPLANATION_CARD_CURIOSITIES)
	}

	boolean hasClosedCuriositiesExplanation() {
		return get(CLOSED_EXPLANATION_CARD_CURIOSITIES)
	}

	void closeTrackathonExplanation() {
		set(CLOSED_EXPLANATION_CARD_TRACKATHON)
	}

	boolean hasClosedTrackathonExplanation() {
		return get(CLOSED_EXPLANATION_CARD_TRACKATHON)
	}

	boolean hasVisitedTrackathon() {
		return get(TRACKATHON_VISITED)
	}

	boolean markTrackathonVisited() {
		set(TRACKATHON_VISITED)
	}

	boolean isDeviceEntriesCollapsed(ThirdParty thirdParty) {
		return get(getDeviceBit(thirdParty))
	}

	boolean collapseDeviceEntries(ThirdParty thirdParty) {
		set(getDeviceBit(thirdParty))
	}

	boolean expandDeviceEntries(ThirdParty thirdParty) {
		clear(getDeviceBit(thirdParty))
	}

	/**
	 * Get the device bit from above defined constants by appending the "_COLLAPSED" to the name of the third party
	 * enum.
	 * @param thirdParty Get UserSettings bit for given third party device.
	 */
	private int getDeviceBit(ThirdParty thirdParty) {
		return UserSettings[thirdParty.name() + "_COLLAPSED"]
	}

	Map<String, Boolean> getDeviceSettings() {
		List<ThirdParty> supportedDevices = [ThirdParty.WITHINGS, ThirdParty.FITBIT, ThirdParty.JAWBONE,
				ThirdParty.MOVES, ThirdParty.OURA]

		return supportedDevices.collectEntries { [(it.name()): isDeviceEntriesCollapsed(it)] }
	}
}
