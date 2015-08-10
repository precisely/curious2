package us.wearecurio.data

import java.util.Date
import java.util.Map
import java.util.concurrent.ConcurrentHashMap

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

import us.wearecurio.data.UnitGroupMap.UnitRatio
import us.wearecurio.datetime.LocalTimeRepeater



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

// repeat type is now an integer flag variable

class RepeatType {
	// IMPORTANT: there must be ghost entries
	// for all non-ghost entries and vice-versa
	// if you add more remind types, please edit the sql in
	// RemindEmailService
	private static final Map<Long, RepeatType> map = new ConcurrentHashMap<Long, RepeatType>(new HashMap<Integer, RepeatType>())
	
	static RepeatType DAILY = new RepeatType(DAILY_BIT)
	static RepeatType WEEKLY = new RepeatType(WEEKLY_BIT)
	static RepeatType DAILYGHOST = new RepeatType(DAILY_BIT | GHOST_BIT)
	static RepeatType WEEKLYGHOST = new RepeatType(WEEKLY_BIT | GHOST_BIT)
	static RepeatType REMINDDAILY = new RepeatType(REMIND_BIT | DAILY_BIT)
	static RepeatType REMINDWEEKLY = new RepeatType(REMIND_BIT | WEEKLY_BIT)
	static RepeatType REMINDDAILYGHOST = new RepeatType(REMIND_BIT | DAILY_BIT | GHOST_BIT)
	static RepeatType REMINDWEEKLYGHOST = new RepeatType(REMIND_BIT | WEEKLY_BIT | GHOST_BIT)
	
	static RepeatType CONTINUOUS = new RepeatType(CONTINUOUS_BIT)
	static RepeatType CONTINUOUSGHOST = new RepeatType(CONTINUOUS_BIT|  GHOST_BIT)
	
	static RepeatType DAILYCONCRETEGHOST = new RepeatType(CONCRETEGHOST_BIT | DAILY_BIT)
	static RepeatType DAILYCONCRETEGHOSTGHOST = new RepeatType(CONCRETEGHOST_BIT | GHOST_BIT | DAILY_BIT)
	static RepeatType WEEKLYCONCRETEGHOST = new RepeatType(CONCRETEGHOST_BIT | WEEKLY_BIT)
	static RepeatType WEEKLYCONCRETEGHOSTGHOST = new RepeatType(CONCRETEGHOST_BIT | GHOST_BIT | WEEKLY_BIT)
	
	static RepeatType GHOST = new RepeatType(GHOST_BIT)
	static RepeatType NOTHING = new RepeatType(0)
	
	static RepeatType DURATIONGHOST = new RepeatType(GHOST_BIT | DURATION_BIT)
	
	static final int DAILY_BIT = 1
	static final int WEEKLY_BIT = 2
	static final int REMIND_BIT = 4
	static final int HOURLY_BIT = 8
	static final int MONTHLY_BIT = 0x0010
	static final int YEARLY_BIT = 0x0020
	static final int CONTINUOUS_BIT = 0x0100
	static final int GHOST_BIT = 0x0200
	static final int CONCRETEGHOST_BIT = 0x0400
	static final int DURATION_BIT = 0x0800
	
	static final int SUNDAY_BIT = 0x10000
	static final int MONDAY_BIT = 0x20000
	static final int TUESDAY_BIT = 0x40000
	static final int WEDNESDAY_BIT = 0x80000
	static final int THURSDAY_BIT = 0x100000
	static final int FRIDAY_BIT = 0x200000
	static final int SATURDAY_BIT = 0x400000
	
	static final int WEEKDAY_BITS = SUNDAY_BIT | MONDAY_BIT | TUESDAY_BIT | WEDNESDAY_BIT | THURSDAY_BIT | FRIDAY_BIT | SATURDAY_BIT
	static final int REPEAT_BITS = HOURLY_BIT | DAILY_BIT | WEEKLY_BIT | MONTHLY_BIT | YEARLY_BIT | WEEKDAY_BITS

	Long id
	
	static RepeatType look(Long id) {
		return get(id)
	}
	
	static RepeatType get(Long id) {
		if (id == null) return null
		RepeatType repeatType = map.get(id)
		if (repeatType == null)
			return new RepeatType(id)
		
		return repeatType
	}
	
	RepeatType(long id) {
		this.id = id
		map.put(id, this)
	}
	
	boolean isGhost() {
		return (this.id & GHOST_BIT) > 0
	}
	
	boolean isConcreteGhost() {
		return (this.id & CONCRETEGHOST_BIT) > 0
	}
	
	boolean isAnyGhost() {
		return (this.id & (GHOST_BIT | CONCRETEGHOST_BIT)) > 0
	}
	
	boolean isContinuous() {
		return (this.id & CONTINUOUS_BIT) > 0
	}
	
	boolean isHourly() {
		return (this.id & HOURLY_BIT) > 0
	}
	
	boolean isDaily() {
		return (this.id & DAILY_BIT) > 0
	}
	
	boolean isWeekly() {
		return (this.id & WEEKLY_BIT) > 0
	}
	
	boolean isMonthly() {
		return (this.id & MONTHLY_BIT) > 0
	}
	
	boolean isYearly() {
		return (this.id & YEARLY_BIT) > 0
	}
	
	boolean isReminder() {
		return (this.id & REMIND_BIT) > 0
	}
	
	boolean isTimed() {
		return (this.id & (DAILY_BIT | WEEKLY_BIT | REMIND_BIT)) > 0
	}
	
	boolean isRepeat() {
		return (this.id & (DAILY_BIT | WEEKLY_BIT | REMIND_BIT | CONTINUOUS_BIT)) > 0
	}
	
	RepeatType unGhost() {
		return RepeatType.look(this.id & (~GHOST_BIT))
	}
	
	RepeatType toggleGhost() {
		if (isGhost())
			return map.get(this.id & (~GHOST_BIT))
		return map.get(this.id | GHOST_BIT)
	}
	
	RepeatType makeGhost() {
		return map.get(this.id | GHOST_BIT)
	}
	
	RepeatType makeConcreteGhost() {
		return map.get(this.id | CONCRETEGHOST_BIT)
	}
	
	int hashCode() {
		return (int)id
	}
	
	boolean equals(Object other) {
		if (other instanceof RepeatType) {
			return ((RepeatType)other).id == this.id
		}
		
		return false
	}
}
