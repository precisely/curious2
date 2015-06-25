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
	public final static String DEFAULT_DURATION_TYPE = Entry.DurationType.NONE
	public final static String DEFAULT_REPEAT_TYPE = null
	public final static String DEFAULT_REPEAT_END = null
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
		def units = entry_params['units'] ?: DEFAULT_UNITS
		def repeatType = entry_params['repeatType'] ?: DEFAULT_REPEAT_TYPE
		def tag_description = entry_params['tag_description'] ?: tag.description
		def baseTag = tag
		def durationType = Entry.DurationType.NONE
		if (tag_description.endsWith(" start")) {
			durationType = Entry.DurationType.START
			baseTag = Tag.look(tag_description.substring(0, tag_description.length() - 6))
		} else if (tag_description.endsWith(" end")) {
			durationType = Entry.DurationType.END
			baseTag = Tag.look(tag_description.substring(0, tag_description.length() - 4))
		} else if (tag_description.endsWith(" stop")) {
			durationType = Entry.DurationType.END
			baseTag = Tag.look(tag_description.substring(0, tag_description.length() - 5))
		}
		def init_params = [
			tag: tag,
			baseTag: baseTag,
			date: entry_params['time'] ?: START_DAY,
			user: user,
			amount: entry_params['amount'] ?: DEFAULT_AMOUNT,
			durationType: durationType,
			repeat: repeatType,
			repeatEnd: entry_params['repeatEnd'] ?: DEFAULT_REPEAT_END,
			datePrecisionSecs: entry_params['datePrecisionSecs'] ?: Entry.DEFAULT_DATEPRECISION_SECS,
			units: units,
			comment: entry_params['comment'] ?: DEFAULT_COMMENT,
			setIdentifier: null
		]
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

	public static def makeStart(baseTagDescription, dateString) {
		def entry0 = EntryFactory.make('tag_description': "${baseTagDescription} start")
		entry0.amount = 100
		entry0.units = "hours"
		entry0.date = Date.parse( 'dd-MM-yyyy HH:mm', dateString )
		entry0
	}

	public static def makeStop(baseTagDescription, dateString) {
		def entry0 = EntryFactory.make('tag_description': "${baseTagDescription} stop")
		entry0.amount = 100
		entry0.units = "hours"
		entry0.date = Date.parse( 'dd-MM-yyyy HH:mm', dateString )
		entry0
	}

}

