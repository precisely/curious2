package us.wearecurio.support

import java.util.Date
import java.util.Map

import us.wearecurio.model.Entry
import us.wearecurio.model.EntryGroup

/*
 * Class for creating entry groups for grouped entries
 */
class EntryCreateMap {
	static mapWith = "none"
	
	Map<Date, Entry> entries = new HashMap<Date, Entry>()
	
	EntryCreateMap() {
	}

	EntryGroup groupForDate(Date date) {
		Entry entry = entries.get(date)
		
		if ((entry != null) && (entry.getUserId() != null)) {
			return entry.createOrFetchGroup()
		}
		
		return null
	}
	
	void add(Entry entry) {
		entries.put(entry.getDate(), entry)
	}
}
