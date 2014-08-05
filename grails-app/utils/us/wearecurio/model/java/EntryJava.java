package us.wearecurio.model.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import us.wearecurio.model.Entry;
import us.wearecurio.parse.ParseUtils;
import us.wearecurio.parse.WordMatcher;
import us.wearecurio.units.UnitGroupMap;

public class EntryJava {
	public static final int DAILY_BIT = 1;
	public static final int WEEKLY_BIT = 2;
	public static final int REMIND_BIT = 4;
	public static final int CONTINUOUS_BIT = 0x0100;
	public static final int GHOST_BIT = 0x0200;
	public static final int CONCRETEGHOST_BIT = 0x0400;
	
	// NOTE: Unghosted repeat entries are repeated in an unghosted manner all
	// the way until their repeatEnd
	// but unghosted remind entries are only unghosted on their start date, and
	// not on subsequent dates
	
	// new end-of-entry repeat indicators:
	// repeat daily
	// delete repeat indicator to end repeat sequence
	// OR: repeat end
	
	// like a ghost, but it appears in plot data, it is just a repetition of a
	// prior
	// entry, must be activated to edit
	
	public static enum RepeatType { // IMPORTANT: there must be ghost entries
		// for all non-ghost entries and vice-versa
		// if you add more remind types, please edit the sql in
		// RemindEmailService
		DAILY(DAILY_BIT, 24L * 60L * 60000), WEEKLY(WEEKLY_BIT,
				7L * 24L * 60L * 60000), DAILYGHOST(DAILY_BIT | GHOST_BIT,
				24L * 60L * 60000), WEEKLYGHOST(WEEKLY_BIT | GHOST_BIT,
				7L * 24L * 60L * 60000), REMINDDAILY(REMIND_BIT | DAILY_BIT,
				24L * 60L * 60000), REMINDWEEKLY(REMIND_BIT | WEEKLY_BIT,
				7L * 24L * 60L * 60000), REMINDDAILYGHOST(REMIND_BIT
				| DAILY_BIT | GHOST_BIT, 24L * 60L * 60000), REMINDWEEKLYGHOST(
				REMIND_BIT | WEEKLY_BIT | GHOST_BIT, 7L * 24L * 60L * 60000),
		
		CONTINUOUS(CONTINUOUS_BIT, 1), CONTINUOUSGHOST(CONTINUOUS_BIT
				| GHOST_BIT, 1),
		
		DAILYCONCRETEGHOST(CONCRETEGHOST_BIT | DAILY_BIT, 24L * 60L * 60000), DAILYCONCRETEGHOSTGHOST(
				CONCRETEGHOST_BIT | GHOST_BIT | DAILY_BIT, 24L * 60L * 60000), WEEKLYCONCRETEGHOST(
				CONCRETEGHOST_BIT | WEEKLY_BIT, 7L * 24L * 60L * 60000), WEEKLYCONCRETEGHOSTGHOST(
				CONCRETEGHOST_BIT | GHOST_BIT | WEEKLY_BIT,
				7L * 24L * 60L * 60000),
		
		GHOST(GHOST_BIT, 0), NOTHING(0, 0);
		
		final Integer id;
		final long duration;
		
		private static final Map<Integer, RepeatType> map = new HashMap<Integer, RepeatType>();
		
		static {
			for (RepeatType type : RepeatType.values()) {
				map.put(type.getId(), type);
			}
		}
		
		private RepeatType(int id, long duration) {
			this.id = id;
			this.duration = duration;
		}
		
		static RepeatType get(int id) {
			return map.get(id);
		}
		
		public int getId() {
			return id;
		}
		
		public boolean isGhost() {
			return (this.id & GHOST_BIT) > 0;
		}
		
		public boolean isConcreteGhost() {
			return (this.id & CONCRETEGHOST_BIT) > 0;
		}
		
		public boolean isAnyGhost() {
			return (this.id & (GHOST_BIT | CONCRETEGHOST_BIT)) > 0;
		}
		
		public boolean isContinuous() {
			return (this.id & CONTINUOUS_BIT) > 0;
		}
		
		public boolean isDaily() {
			return (this.id & DAILY_BIT) > 0;
		}
		
		public boolean isWeekly() {
			return (this.id & WEEKLY_BIT) > 0;
		}
		
		public boolean isReminder() {
			return (this.id & REMIND_BIT) > 0;
		}
		
		public boolean isTimed() {
			return (this.id & (DAILY_BIT | WEEKLY_BIT | REMIND_BIT)) > 0;
		}
		
		public boolean isRepeat() {
			return (this.id & (DAILY_BIT | WEEKLY_BIT | REMIND_BIT | CONTINUOUS_BIT)) > 0;
		}
		
		public RepeatType unGhost() {
			return RepeatType.get(this.id & (~GHOST_BIT));
		}
		
		public RepeatType toggleGhost() {
			if (isGhost())
				return RepeatType.get(this.id & (~GHOST_BIT));
			return RepeatType.get(this.id | GHOST_BIT);
		}
		
		public RepeatType makeGhost() {
			return RepeatType.get(this.id | GHOST_BIT);
		}
		
		public RepeatType makeConcreteGhost() {
			return RepeatType.get(this.id | CONCRETEGHOST_BIT);
		}
		
		public RepeatType forUpdate() {
			if (isReminder())
				return RepeatType.get(this.id
						& (~(GHOST_BIT | CONCRETEGHOST_BIT)));
			return this;
		}
	}
	
	public static int DURATIONSTART_BIT = 1;
	public static int DURATIONEND_BIT = 2;
	public static int DURATIONGHOST_BIT = 16;
	
	public static enum DurationType {
		NONE(0), START(DURATIONSTART_BIT), END(DURATIONEND_BIT),
				GHOSTSTART(DURATIONSTART_BIT | DURATIONGHOST_BIT),
				GHOSTEND(DURATIONEND_BIT | DURATIONGHOST_BIT);

		final Integer id;

		DurationType(Integer id) {
			this.id = id;
		}
		
		public boolean isGhost() {
			return (this.id & DURATIONGHOST_BIT) != 0;
		}
		
		public DurationType unGhost() {
			if (this == GHOSTSTART) return START;
			else if (this == GHOSTEND) return END;
			
			return this;
		}
		
		public DurationType makeGhost() {
			if (this == START) return GHOSTSTART;
			else if (this == END) return GHOSTEND;
			
			return this;
		}
	}
	
	protected static final WordMatcher<RepeatType> repeatWordMatcher = new WordMatcher<RepeatType>().addAll(Entry.repeatWords);
	protected static final WordMatcher<ArrayList<Object>> durationSynonymMatcher = new WordMatcher<ArrayList<Object>>().addAll(Entry.durationSynonyms);
	protected static final WordMatcher<ArrayList<Object>> durationSuffixMatcher = new WordMatcher<ArrayList<Object>>().addAll(Entry.durationSuffixes);
	
	public static boolean parseSuffix(LinkedList<String> words, Map<String, Object> retVal) {
		int wordsSize = words.size();
		
		if (wordsSize <= 1)
			return false; // need at least two words to match a tag modifier
			
		// try matching repeat words at the beginning
		RepeatType repeatType = repeatWordMatcher.matchRemove(words, 0);
		
		if (repeatType != null) {
			retVal.put("repeatType", repeatType);
			return true;
		}
		
		// try matching repeat words at the end
		if (wordsSize > 2) {
			repeatType = repeatWordMatcher.matchRemove(words, wordsSize - 2);
			
			if (repeatType != null) {
				retVal.put("repeatType", repeatType);
				return true;
			}
			
			repeatType = repeatWordMatcher.matchRemove(words, wordsSize - 3);
			
			if (repeatType != null) {
				retVal.put("repeatType", repeatType);
				return true;
			}
		}
		
		// try matching duration words at the beginning or end
		ArrayList<Object> durationSynonym = durationSuffixMatcher.matchRemove(words, 0);
		
		if (durationSynonym == null)
			durationSynonym = durationSuffixMatcher.matchRemove(words, wordsSize - 1);
		
		if (durationSynonym != null) { // append canonical duration suffix
			String baseTagDescription = ParseUtils.implode(words, " ");
			words.addLast((String)durationSynonym.get(0));
			retVal.put("durationType", (DurationType)durationSynonym.get(1));
			retVal.put("baseTagDescription", baseTagDescription);
			return true;
		}
		
		return false;
	}
	
	public static boolean parseTagSuffix(LinkedList<String> words, Map<String, Object> retVal) {
		int wordsSize = words.size();
		
		if (wordsSize <= 1)
			return false; // need at least two words to match a tag modifier
		
		if (parseSuffix(words, retVal)) return true;
		
		// try matching duration synonyms at the beginning
		ArrayList<Object> durationValue = durationSynonymMatcher.matchRemove(words, 0);
		
		if (durationValue != null) {
			// replace matched words with synonym
			words.addFirst((String) durationValue.get(1));
			words.addFirst((String) durationValue.get(0));
			retVal.put("durationType", (DurationType) durationValue.get(2));
			return true;
		}
		
		// try matching unit suffixes at the end of the tag name
		if (UnitGroupMap.theMap.getSuffixes().contains(words.getLast())) {
			String baseTagDescription = ParseUtils.implode(words, " ", wordsSize - 1);
			retVal.put("baseTagDescription", baseTagDescription);
			return true;
		}
		
		return false;
	}
}
