package us.wearecurio.model

import groovy.time.*
import grails.converters.*

import org.apache.commons.logging.LogFactory

import us.wearecurio.parse.ParseUtils
import us.wearecurio.datetime.LocalTimeRepeater
import us.wearecurio.parse.PatternScanner
import us.wearecurio.services.DatabaseService
import us.wearecurio.utility.Utils
import us.wearecurio.model.Tag
import us.wearecurio.units.UnitGroupMap
import us.wearecurio.units.UnitGroupMap.UnitRatio

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.*
import org.junit.Before;

class EntryIdGroup {

	private static def log = LogFactory.getLog(this)
	
	static EntryIdGroup create(Entry firstEntry) {
		EntryIdGroup group = new EntryIdGroup()
		group.add(firstEntry)
		
		return group
	}
	
	static void delete(EntryIdGroup group) {
		group.delete()
	}
	
	Set<Long> entryIds = new HashSet<Long>()
	TreeSet<Entry> groupEntries // transient cache of group members
	
	static transients = [ 'groupEntries' ]
	
	void add(Entry e) {
		save()
		Utils.save(e, true)
		entryIds.add(e.getId())
		fetchSortedEntries().add(e)
	}
	
	void remove(Entry e) {
		entryIds.remove(e.getId())
		groupEntries.remove(e)
	}
	
	void resort(Entry e) {
		fetchSortedEntries()
		
		groupEntries.remove(e)
		groupEntries.add(e)
	}
	
	protected TreeSet<Entry> computeSortedEntries() {
		TreeSet<Entry> entries = new TreeSet<Entry>()
		for (Long entryId : entryIds) {
			entries.add(Entry.get(entryId))
		}
		
		return entries
	}
	
	TreeSet<Entry> fetchSortedEntries() {
		if (groupEntries != null)
			return groupEntries
			
		return groupEntries = computeSortedEntries()
	}
}
