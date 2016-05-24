package us.wearecurio.support

import java.util.Date

import us.wearecurio.model.Entry
import us.wearecurio.model.EntryGroup

/*
 * Class for creating entry groups for grouped entries
 */
class EntryCreateMap {
	static mapWith = "none"

	Map<String, Entry> entries = new HashMap<String, Entry>()

	EntryCreateMap() {
	}

	EntryGroup groupForDate(Date date, String baseTag) {
		Entry entry = entries.get(date.getTime() + "+" + baseTag)

		if ((entry != null) && (entry.getUserId() != null)) {
			return entry.createOrFetchGroup()
		}

		return null
	}

	void add(Entry entry) {
		if (entry != null)
			entries.put(entry.getDate().getTime() + "+" + entry.baseTag?.description, entry)
	}
}
