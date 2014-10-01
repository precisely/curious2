package us.wearecurio.model
import us.wearecurio.utility.Utils
import org.hibernate.criterion.Restrictions as R
import org.hibernate.criterion.Order as O

class Correlation {

	Long id
	String series1Type
	Long series1Id
	String series2Type
	Long series2Id
	Long userId
	Double corValue
	Double mipssValue
	Double triggerValue

	Double overlapN
	Date created
	Date updated

	Date saved
	Date noise
	Date viewed

	static constraints = {
		corValue nullable: true //, validator: { val, obj -> val && !Double.isNaN(val) }
		overlapN nullable: true
		mipssValue nullable: true
		triggerValue nullable: true
		created nullable: true
		updated nullable: true
		saved nullable: true
		noise nullable: true
		viewed nullable: true
	}

	static mapping = {
		version false
	}

	public Correlation(CuriousSeries series1, CuriousSeries series2) {
		userId = series1.userId
		series1Type = series1.source.toString()
		series2Type = series2.source.toString()
		series1Id = series1.sourceId
		series2Id = series2.sourceId
		created = new Date()
		updated = new Date()
	}

	public Correlation(CuriousSeries series1, CuriousSeries series2, Double corValue) {
		userId = series1.userId
		series1Type = series1.source.toString()
		series2Type = series2.source.toString()
		series1Id = series1.sourceId
		series2Id = series2.sourceId

		if (Double.isNaN(corValue)) {
			corValue = null
		}

		this.corValue = corValue
		created = new Date()
		updated = new Date()
	}

	public static userCorrelations(Long userId, Integer max, String flavor) {
		def criteria = Correlation.createCriteria()
		criteria = criteria.add( R.eq("userId", userId) )
		def column = null
		switch(flavor) {

			case "triggered":
				column = "triggerValue"
				break;

			case "saved":
				column = "saved"
				break

			default:
				column = "mipssValue"
				break
		}
		def orderRestriction = (flavor == "negative" ? O.asc(column) : O.desc(column))
		criteria.addOrder( orderRestriction )
		criteria = criteria.setMaxResults( max )
		criteria.list()
	}

	public static markViewed(Long theId) {
		Correlation correlation = Correlation.get(theId)
		correlation.viewed = new Date()
		Utils.save(correlation, true)
	}

	public static markNoise(Long theId) {
		Correlation correlation = Correlation.get(theId)
		correlation.noise = new Date()
		Utils.save(correlation, true)
	}

	public static markSaved(Long theId) {
		Correlation correlation = Correlation.get(theId)
		correlation.saved = new Date()
		Utils.save(correlation, true)
	}

}
