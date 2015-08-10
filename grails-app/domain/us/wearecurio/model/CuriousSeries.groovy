package us.wearecurio.model

import us.wearecurio.model.Entry
import us.wearecurio.model.Stats
import us.wearecurio.data.RepeatType

class CuriousSeries {
	// This class is mainly to massage and normalize Tag or TagGroup entries into a form that is
	//	 easily consumable by statistical computations.  For example, if the sample frequency is
	//	 daily, it will average multiple values on a given day.
	// If it's a duration entry, it will sum multiple values on a given day.
	// Another example of what this class can do is take multiple tags can combine them into a
	//	single series in a reasonable way.

	static mapping = {
		version false
	}
	
	ArrayList<Entry> entries
	def values
	def times

	// Source of values.
	def source
	def sourceId
	def sourceDescription
	def userId

	// Don't need to save CuriousSeries to the database.
	//def isAttached() { false }

	// Create a CuriousSeries from a list of entry values corresponding
	//	to a single tag.
	public CuriousSeries(ArrayList<Entry> list_of_entries, source=Tag, sourceId=null, sourceDescription=null, userId=null) {
		// Identify where the entries are coming from, e.g. a Tag or a TagGroup.
		this.source = source
		this.sourceId = sourceId
		this.sourceDescription = sourceDescription
		this.userId = userId
		entries = list_of_entries
		entries = initialize_entries(entries)
		times = resetTimes()
		values = resetValues()
	}

	public static CuriousSeries create(List<Entry> entries, source=Tag, sourceId=null, sourceDescription=null, userId=null) {
		// Filter out entries with null dates and values.
		def list_of_entries = CuriousSeries.prefilterEntries(entries)

		if (list_of_entries.size() == 0) {
			return null
		} else {
			return new CuriousSeries(list_of_entries, source, sourceId, sourceDescription, userId)
		}
	}

	public static initialize_entries(entries) {
		def generated_repeat_entries = []
		// Sort by date descending, then durationType ascending.
		entries = entries.sort({ a,b-> a.date < b.date ? -1 : (a.date > b.date ?	1 : a.durationType < b.durationType ? 1 : -1 )})
		def last_start_tag = [:]
		def duration_end_entries = []
		for (entry in entries) {
			// PROCESS REPEAT_TYPE entries
			if (entry.repeatType == RepeatType.DAILY)  {
				// Make a list of generated values that we'll append later.
				generated_repeat_entries += createRepeatedEntries(entry)

			// PRE-PROCESS (map) DURATION_TYPE entries
			} else if (entry.durationType.isStartOrEnd()) {
				if (entry.durationType.isEnd()) {
					if (entry && entry.date && entry.baseTagId && last_start_tag[entry.baseTagId]) {
						entry.amount = entry.date.getTime() - last_start_tag[entry.baseTagId].date.getTime()
						duration_end_entries << entry
					} else {
						entry.amount = null
					}
				} else { // This is a START tag.
					// For O(1) retrieval of the corresonding start entry.
					last_start_tag[entry.baseTagId] = entry
					// This will cause the entry to be tossed out later.	Since we just need the date
					//	we don't care about the amount.
					entry.amount = null
				}

			} else if ( entry.repeatType == RepeatType.DAILYGHOST ||
									entry.repeatType == RepeatType.REMINDDAILYGHOST ||
									entry.repeatType == RepeatType.REMINDWEEKLYGHOST ||
									entry.repeatType == RepeatType.CONTINUOUS ||
									entry.repeatType == RepeatType.CONTINUOUSGHOST ||
									entry.repeatType == RepeatType.DAILYCONCRETEGHOSTGHOST ) {
				entry.amount = null
			}
		}

		//// POST-PROCESS REPEAT_TYPE entries.
		entries += generated_repeat_entries

		//// POST-PROCESS (reduce) durationType.END entries and sum up the day's value.
		def daily_values = [:]
		for (entry in duration_end_entries) {
			def day = entry.date.clearTime()
			if (daily_values[day] == null) {
				daily_values[day] = entry
			} else {
				daily_values[day].amount += entry.amount
				entry.amount = null
			}
		}
		entries
	}

	public static createRepeatedEntries(entry) {
		def repeats = []
		def now = new Date()
		def start_day = entry.date
		def last_day
		if (entry.repeatEnd == null) {
			last_day = now
		} else {
			last_day = entry.repeatEnd < now ? entry.repeatEnd : now
		}
		def num_days = last_day - start_day
		for (def i=1; i <= num_days; i++) {
			def new_entry = new Entry(date: start_day + i, amount: entry.amount)
			repeats << new_entry
		}
		repeats
	}

	public static CuriousSeries create(tag, userId) {
		def entries = Entry.fetchByUserAndTag(userId, tag.description)
		CuriousSeries.create(entries, Tag, tag.id, tag.description, userId)
	}

	public static prefilterEntries(def entries) {
		entries.grep { it.amount != null && it.date != null }
	}

	public static day_values(def first_day, def last_day) {
		first_day = first_day.clearTime()
		last_day = last_day.clearTime()
		def length = last_day - first_day + 1
		def days = new Date[length]
		for (def cur_day = first_day; cur_day <= last_day; cur_day += 1) {
			days[cur_day - first_day] = cur_day
		}
		days
	}

	public static Entry firstEntry(List entries) {
		Entry first
		for (entry in entries) {
			if (first == null) {
				first = entry
			} else if (first.date > entry.date) {
				first = entry
			}
		}
		first
	}

	public static Entry lastEntry(entries) {
		Entry last
		for (entry in entries) {
			if (last == null) {
				last = entry
			} else if (last.date < entry.date) {
				last = entry
			}
		}
		last
	}

	def resetTimes() {
		def first_day = CuriousSeries.firstEntry(entries)
		first_day = first_day ? first_day.date : null
		if (first_day == null) { return [] }
		def last_day = CuriousSeries.lastEntry(entries).date
		CuriousSeries.day_values(first_day, last_day)
	}

	def resetValues() {
		Map entriesByTimeInterval = [:]
		for(entry in entries) {
			def key = entry.date.clearTime()
			entriesByTimeInterval[key] = entriesByTimeInterval[key] ?: []
			entriesByTimeInterval[key] += entry
		}
		def values = new Double[times.size()]
		def first_day = times[0]
		for (time in times) {
			def pos = time - first_day
			if (entriesByTimeInterval[time] != null) {
				def amounts = entriesByTimeInterval[time].collect { it.amount }
				values[pos] = Stats.mean(amounts)
			}
		}
		values
	}

	def size() {
		values.size()
	}

	def start_day() {
		times && times.size() > 0 ? times[0] : null
	}

	def stop_day() {
		times && times.size() > 0 ? times[times.size() - 1] : null
	}

	public static mergedTimes(CuriousSeries series1, CuriousSeries series2) {
		def first_day = series1.times.first()
		def last_day = series1.times.last()
		if (series2.times.first() < first_day) {
			first_day = series2.times.first()
		}
		if (series2.times.last() > last_day) {
			last_day = series2.times.last()
		}
		CuriousSeries.day_values(first_day, last_day)
	}

	public static valuesOn(CuriousSeries series, timeDomain) {
		def first_day_of_series = series.times.first()
		def new_values = timeDomain.collect {
			def i = it - first_day_of_series
			if (0 <= i && i < series.size()) {
				series.values[i]
			} else {
				null
			}
		}
	}

	public static sizeNotNull(CuriousSeries series1, CuriousSeries series2) {
		def timeDomain = mergedTimes(series1, series2)
		def series1_values = valuesOn(series1, timeDomain)
		def series2_values = valuesOn(series2, timeDomain)
		def dotted = Stats.dot(series1_values, series2_values)
		dotted.removeAll([null])
		dotted.size()
	}

	public static cor(CuriousSeries series1, CuriousSeries series2) {
		def timeDomain = mergedTimes(series1, series2)
		def series1_values = valuesOn(series1, timeDomain)
		def series2_values = valuesOn(series2, timeDomain)
		Stats.cor(series1_values, series2_values)
	}

	public static mipss(CuriousSeries series1, CuriousSeries series2) {
		if (series1 == null || series2 == null) return null
		def timeDomain = mergedTimes(series1, series2)
		def series1_values = valuesOn(series1, timeDomain)
		def series2_values = valuesOn(series2, timeDomain)
		Stats.mipss(series1_values, series2_values)
	}


} // class
