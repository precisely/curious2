package us.wearecurio.model

import us.wearecurio.model.Entry
import us.wearecurio.model.Stats
import us.wearecurio.services.EntryService
import org.apache.commons.logging.LogFactory

class Series {
  private static def log = LogFactory.getLog(this)
  // This class is mainly to massage and normalize Tag or TagGroup entries into a form that is
  //   easily consumable by statistical computations.  For example, if the sample frequency is
  //   daily, it will average multiple values on a given day.
  // Another example of what this class can do is take multiple tags can combine them into a
  //  single series in a reasonable way.
	private static def entryService = new EntryService()

  ArrayList<Entry> entries
  def values
  def times
  
  // Source of values.
  def source
  def sourceId
  def userId
  
  // Don't need to save Series to the database.
  def isAttached() { false }

//  public Series() {
//    values = []
//    times = []
//  }

  // Create a Series from a list of entry values corresponding
  //  to a single tag.
  public Series(ArrayList<Entry> list_of_entries, source=Tag, sourceId=null, userId=null) {
    entries = list_of_entries
	  times = resetTimes()
	  // Once entries is set, we can compute times and values.
    
	  values = resetValues()
	  // Identify where the entries are coming from, e.g. a Tag or a TagGroup.
	  this.source = source
	  this.sourceId = sourceId
	  this.userId = userId
  }

  public static def create(tag, userId) {
    def entries = Series.entryService.getEntriesByUserAndTag(userId, tag.description)
    new Series(entries, Tag, tag.id, userId)
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

  public static Entry firstEntry(entries) {
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
	  def first_day = Series.firstEntry(entries).date
	  def last_day = Series.lastEntry(entries).date
    this.times = Series.day_values(first_day, last_day)
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

  public static mergedTimes(Series series1, Series series2) {
    def first_day = series1.times.first()
    def last_day = series1.times.last()
    if (series2.times.first() < first_day) {
      first_day = series2.times.first()
    }
    if (series2.times.last() > last_day) {
      last_day = series2.times.last()
    }
    Series.day_values(first_day, last_day)
  }

  public static valuesOn(Series series, timeDomain) {
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

  public static cor(Series series1, series2) {
    def timeDomain = mergedTimes(series1, series2)
    def series1_values = valuesOn(series1, timeDomain)
    def series2_values = valuesOn(series2, timeDomain)
    if (Stats.sizeNotNull(series1_values, series2_values) < 2) {
      return null
    }
    Stats.cor(series1_values, series2_values)
  }


} // class
