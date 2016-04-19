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

	private static final int FIRST_ENTRY = 11
	private static final int SECOND_ENTRY = 12
	private static final int TRACKATHON_STARTED_1 = 13
	private static final int TRACKATHON_STARTED_2 = 14
	private static final int TRACKATHON_STARTED_3 = 15
	private static final int DISCUSSION_DETAIL_VISIT_1 = 16
	private static final int DISCUSSION_DETAIL_VISIT_2 = 17
	private static final int DISCUSSION_DETAIL_VISIT_3 = 18
	private static final int BOOKMARK_CREATED_1 = 19
	private static final int BOOKMARK_CREATED_2 = 20
	private static final int BOOKMARK_CREATED_3 = 21
	private static final int FIRST_ALERT_ENTRY = 22
	// This bit is set when a user logs in to the mobile app for the first time (or register in the mobile app).
	private static final int VISITED_MOBILE_APP = 23

	/**
	 * Do not save following fields into the database. In a embedded groovy domain class, Hibernate try to persist
	 * the following fields to the database since the getters of this fields are defined.
	 */
	static transients = ["bioPublic", "namePublic", "value", "deviceEntryStates"]

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

	void markTrackathonVisited() {
		set(TRACKATHON_VISITED)
	}
	
	boolean hasVisitedTrackathon() {
		return get(TRACKATHON_VISITED)
	}
	
	void countEntryCreation() {
		if (!get(FIRST_ENTRY)) {
			set(FIRST_ENTRY)
			
			return
		}
		
		if (!get(SECOND_ENTRY)) {
			set(SECOND_ENTRY)
			
			return
		}
	}
	
	boolean hasEntryCreationCountCompleted() {
		return (get(FIRST_ENTRY) && get(SECOND_ENTRY))
	}
	
	void countTrackathonStart() {
		if (!get(TRACKATHON_STARTED_1)) {
			set(TRACKATHON_STARTED_1)
			
			return
		}
	
		if (!get(TRACKATHON_STARTED_2)) {
			set(TRACKATHON_STARTED_2)
			
			return
		}
		
		if (!get(TRACKATHON_STARTED_3)) {
			set(TRACKATHON_STARTED_3)
			
			return
		}
	}
	
	boolean hasTrackathonStartCountCompleted() {
		return (get(TRACKATHON_STARTED_1) && get(TRACKATHON_STARTED_2) && get(TRACKATHON_STARTED_3))
	}
	
	void countDiscussionDetailVisit() {
		if (!get(DISCUSSION_DETAIL_VISIT_1)) {
			set(DISCUSSION_DETAIL_VISIT_1)
			
			return
		}
		
		if (!get(DISCUSSION_DETAIL_VISIT_2)) {
			set(DISCUSSION_DETAIL_VISIT_2)
			
			return
		}
		
		if (!get(DISCUSSION_DETAIL_VISIT_3)) {
			set(DISCUSSION_DETAIL_VISIT_3)
			
			return
		}
	}
	
	boolean hasDiscussionDetailVisitCountCompleted() {
		return (get(DISCUSSION_DETAIL_VISIT_1) && get(DISCUSSION_DETAIL_VISIT_2) && get(DISCUSSION_DETAIL_VISIT_3))
	}
	
	void countBookmarkCreation() {
		if (!get(BOOKMARK_CREATED_1)) {
			set(BOOKMARK_CREATED_1)

			return
		}

		if (!get(BOOKMARK_CREATED_2)) {
			set(BOOKMARK_CREATED_2)

			return
		}

		if (!get(BOOKMARK_CREATED_3)) {
			set(BOOKMARK_CREATED_3)

			return
		}
	}

	boolean hasBookmarkCreationCountCompleted() {
		return (get(BOOKMARK_CREATED_1) && get(BOOKMARK_CREATED_2) && get(BOOKMARK_CREATED_3))
	}
	
	void countFirstAlertEntry() {
		if (!get(FIRST_ALERT_ENTRY)) {
			set(FIRST_ALERT_ENTRY)
		}
	}
	
	boolean hasFirstAlertEntryCountCompleted() {
		return get(FIRST_ALERT_ENTRY)
	}
	
	void markFirstVisit() {
		if (!get(VISITED_MOBILE_APP)) {
			set(VISITED_MOBILE_APP)
		}
	}
	
	boolean hasVisitedMobileApp() {
		return get(VISITED_MOBILE_APP)
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

	Map<String, Boolean> getDeviceEntryStates() {
		List<ThirdParty> supportedDevices = [ThirdParty.WITHINGS, ThirdParty.FITBIT, ThirdParty.JAWBONE,
				ThirdParty.MOVES, ThirdParty.OURA]

		return supportedDevices.collectEntries { [(it.name()): isDeviceEntriesCollapsed(it)] }
	}
}
