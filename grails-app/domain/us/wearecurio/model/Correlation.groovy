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

	// auxilliary information serialized as a JSON string.
	String auxJson


	static constraints = {
		valueType nullable: true
		value nullable: true
		auxJson nullable: true

		saved nullable: true
		noise nullable: true
		viewed nullable: true
		created nullable: true
		updated nullable: true
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
		criteria = criteria.add( R.ltProperty("series1Id", "series2Id") )

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

	def savedAsLong() { saved ? saved.getTime() : null }
	def viewedAsLong() { viewed ? viewed.getTime() : null }
	def noiseAsLong() { noise ? noise.getTime() : null }

	def description1() { Tag.get(series1Id).description }
	def description2() { Tag.get(series2Id).description }

}
