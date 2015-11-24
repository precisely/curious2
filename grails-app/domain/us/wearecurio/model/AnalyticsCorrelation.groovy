package us.wearecurio.model

import us.wearecurio.utility.Utils
import org.hibernate.criterion.Restrictions as R
import org.hibernate.criterion.Order as O
import org.hibernate.criterion.Projections as P

class AnalyticsCorrelation {
	private static final pageSize = 10
	private static final Long minOverlap = 3

	Long id
	String series1Type
	Long series1Id
	String description1

	String series2Type
	Long series2Id
	String description2

	Long userId

	Date created
	Date updated

	Date saved
	Date noise
	Date viewed

	enum ValueType {
		MIPSS(0), COR(1), TRIGGER(2)
		final Integer id
		ValueType(Integer id) {
			this.id = id
		}
	}

	ValueType valueType
	Double value
	Double absValue

	Long overlapN
	String auxJson
	Double signalLevel

	static constraints = {
		valueType nullable: true
		value nullable: true
		absValue nullable: true
		auxJson nullable: true
		overlapN nullable: true
		signalLevel nullable:true
		description1 nullable: true
		description2 nullable: true

		saved nullable: true
		noise nullable: true
		viewed nullable: true
		created nullable: true
		updated nullable: true
	}

	static mapping = {
		version false
		absValue index: 'absValueIdx'
		value index: 'valueIdx'
		overlapN index: 'overlapNIdx'
		userId index: 'updateIdx,overlapNIdx'
		series1Id index: 'updateIdx'
		series2Id index: 'updateIdx'
		series1Type index: 'updateIdx'
		series2Type index: 'updateIdx'
		description1 index: 'search1'
		description2 index: 'search2'
		signalLevel index: 'signalLevelIdx', defaultValue: (Double)-1.0
	}

	public static userCorrelations(Long userId, Integer max, String flavor) {
		def criteria = AnalyticsCorrelation.createCriteria()
		criteria = criteria.add( R.eq("userId", userId) )
		criteria = criteria.add( R.ltProperty("series1Id", "series2Id") )
		criteria = criteria.add( R.ge( "overlapN", minOverlap ) )

		def column = null
		switch(flavor) {

			case "triggered":
				column = "value"
				break;

			case "saved":
				column = "saved"
				criteria.add( R.isNotNull("saved") )
				break

			default:
				column = "value"
				break
		}
		def orderRestriction = (flavor == "negative" ? O.asc(column) : O.desc(column))
		criteria.addOrder( orderRestriction )
		criteria = criteria.setMaxResults( max )
		criteria.list()
	}

	public static updateSignalLevel(Long theId, Double signalLevel) {
		AnalyticsCorrelation correlation = AnalyticsCorrelation.get(theId)
		if (signalLevel == 0) {
			correlation.noise = new Date()
		}
		if (signalLevel >= 4) {
			correlation.saved = new Date()
		}
		correlation.signalLevel = signalLevel
		Utils.save(correlation, true)
	}

	public static markViewed(Long theId) {
		AnalyticsCorrelation correlation = AnalyticsCorrelation.get(theId)
		correlation.viewed = new Date()
		Utils.save(correlation, true)
	}

	public static markSaved(Long theId) {
		AnalyticsCorrelation correlation = AnalyticsCorrelation.get(theId)
		correlation.saved = new Date()
		Utils.save(correlation, true)
	}

	def savedAsLong() { saved ? saved.getTime() : null }
	def viewedAsLong() { viewed ? viewed.getTime() : null }
	def noiseAsLong() { noise ? noise.getTime() : null }

	def description1() { Tag.get(series1Id).description }
	def description2() { Tag.get(series2Id).description }

	public static addRestriction(criteria, order) {
		def (name, asc) = order.tokenize(' ')
		def criterion = criteria;
		// 'alpha' or 'score'
		if (name == 'alpha' || name == 'score') {
			def c = ['alpha': 'description1', 'score': 'value', 'marked': 'signalLevel']
			def or = (asc == "asc" ? O.asc(c[name]) : O.desc(c[name]));
			criterion = criteria.addOrder(or)

		}
		// 'rated'
		else if (name == 'rated') {
			if (asc == 'asc') {
				criterion = criteria.addOrder( O.asc('signalLevel') )
			} else {
				criterion = criteria.addOrder( O.desc('signalLevel') )
			}
			criterion = criteria.addOrder( O.desc('absValue') )
		}
		// 'natural'
		else if (name == 'natural') {
			// Sort by absolute value of 'value' column.
			criterion = criteria.addOrder(O.asc('signalLevel'))
			criterion = criteria.addOrder(O.desc('absValue'))
		} else if (name == 'all') {
			criterion = criteria.addOrder( O.desc('absValue') )
		}
		criterion
	}

	public static addFilter(criteria, filter) {
		def criterion = criteria
		if (filter == 'yes') {
			criterion = criteria.add( R.eq('signalLevel', new Double(4.0) ) )
		} else if (filter == 'no') {
			criterion = criteria.add( R.eq('signalLevel', new Double(0.0) ) )
		} else if (filter == 'natural') {
			criterion = criteria.add( R.lt('signalLevel', new Double(0.0) ) )
		} else if (filter == 'rated') {
			criterion = criteria.add( R.ge('signalLevel', new Double(0.0) ) )
		}
		criterion
	}

	public static search( userId, filter, order1, order2, q, pageNumber) {
		def criteria = AnalyticsCorrelation.createCriteria()
		def resultList = []

		// Restrict results to the current user.
		criteria.add( R.eq("userId", userId) )

		// Add a search query term if any.
		if (q && q.size() > 0) {
			def queryRestriction = R.or(R.ilike("description1", "%${q}%"), R.ilike("description2", "%${q}%"))
			criteria = criteria.add( queryRestriction )
		}

		// Add filter ('all', 'yes', 'no').
		if (filter && filter.size() > 0) {
			criteria = addFilter(criteria, filter)
		}

		if (order1 && order1.size() > 0) {
			criteria = addRestriction(criteria, order1)
			criteria = addFilter(criteria, order1)
			/*if (order2 && order2.size() > 0) {
				criteria = addRestriction(criteria, order1)
			}*/
		}
		criteria = criteria.add( R.ge('absValue', new Double(0.01) ) )
		criteria = criteria.add( R.ltProperty("series1Id", "series2Id") )
		criteria.setFirstResult((pageNumber - 1) * pageSize)
		criteria.setMaxResults( pageSize )
		resultList = criteria.list()
		resultList
	}

	def asJson() {
		[ id: id,
			series1Type: series1Type,
			series2Type: series2Type,
			series1Id: series1Id,
			series2Id: series2Id,
			description1: description1(),
			description2: description2(),
			valueType: valueType,
			value: value,
			signalLevel: signalLevel,
			overlapn: overlapN,
			saved: savedAsLong(),
			noise: noiseAsLong(),
			viewed: viewedAsLong()]
	}
}
