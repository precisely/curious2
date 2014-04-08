package us.wearecurio.factories

import org.apache.commons.logging.LogFactory
import us.wearecurio.model.Entry
import us.wearecurio.model.User
import us.wearecurio.model.Tag
import us.wearecurio.factories.UserFactory
import us.wearecurio.factories.TagFactory


class EntryFactory {
	private static def mylog = LogFactory.getLog(this)
	public final static Long DEFAULT_AMOUNT = 42
	public final static String DEFAULT_UNITS = 'no units'
	public final static String DEFAULT_COMMENT = 'no comment'
	private final static Date START_DAY = (new GregorianCalendar(2000, Calendar.JANUARY, 1)).time
	private final static Map DEFAULT_ENTRY_PARAMS = [amount: DEFAULT_AMOUNT, time: START_DAY, units: DEFAULT_UNITS, comment: DEFAULT_COMMENT]

	public static def make(Map entry_params=DEFAULT_ENTRY_PARAMS) {
	  def user = UserFactory.make(username: entry_params['username'] ?: 'y')
	  def tag = TagFactory.make(entry_params['tag_description'])
	  Entry entry = EntryFactory.makeSaved(user, tag, entry_params)
	  entry
	}

	public static def makeUnsaved(User user, Tag tag, Map entry_params=DEFAULT_ENTRY_PARAMS) {
		entry_params = entry_params ?: [null: null]
    
		def init_params = [tag: tag,
			date: entry_params['time'] ?: START_DAY,
			user: user,
			amount: entry_params['amount'] ?: DEFAULT_AMOUNT,
			timeZoneOffsetSecs: 0,
			units: entry_params['units'] ?: DEFAULT_UNITS,
			comment: entry_params['comment'] ?: DEFAULT_COMMENT]
		Entry entry = new Entry(init_params)
		entry.userId = user.getId()
	    entry
	}

	public static def makeSaved(User user, Tag tag, Map entry_params=DEFAULT_ENTRY_PARAMS) {
		entry_params = entry_params ?: [null: null]
		Entry entry = makeUnsaved(user, tag, entry_params)
		entry.validate()
		entry.save()
    entry
	}

	public static def makeN(Long n, value_func=null, entry_params=DEFAULT_ENTRY_PARAMS) {
		value_func = value_func ?: { Math.sin(it) }
		def day = (new GregorianCalendar(2000, Calendar.JANUARY, 0)).time
		ArrayList entries = (1..n).toArray().collect {
			day += 1
		    make(entry_params + [amount: value_func(it), time: day])
		}
		entries
	}
}

