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
import us.wearecurio.model.Entry
import us.wearecurio.units.UnitGroupMap
import us.wearecurio.units.UnitGroupMap.UnitRatio

import java.util.ArrayList
import java.util.Map;
import java.util.TreeSet
import java.util.regex.Pattern
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.*
import org.junit.Before

class EntryGroup {

	SortedSet entries
	
	static hasMany = [ entries: Entry ]
	
	private static def log = LogFactory.getLog(this)
	
	static mapping = {
		version false
	}
	
	static EntryGroup create(Entry firstEntry) {
		EntryGroup group = new EntryGroup()
		group.addToEntries(firstEntry)
		
		return group
	}
	
	static void delete(EntryGroup group) {
		group.delete()
	}
	
	void add(Entry e) {
		this.addToEntries(e)
		
		this.save()
	}
	
	void remove(Entry e) {
		this.removeFromEntries(e)
			
		this.save()
	}
	
	void resort(Entry e) {
		this.removeFromEntries(e)
		this.addToEntries(e)
		
		this.save()
	}
	
	boolean isEmpty() {
		return entries.isEmpty()
	}
	
	SortedSet<Entry> fetchSortedEntries() {
		return entries
	}
}
