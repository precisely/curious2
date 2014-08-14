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
import java.util.regex.Pattern
import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.*
import org.junit.Before;

class EntryGroup {

	private static def log = LogFactory.getLog(this)
	
	SortedSet entries = new TreeSet<Entry>()

	static hasMany = [entries : Entry]
	
	def add(Entry e) {
		entries.add(e)
	}
	
	def remove(Entry e) {
		entries.remove(e)
	}
}
