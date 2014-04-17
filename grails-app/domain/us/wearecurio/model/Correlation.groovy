package us.wearecurio.model

class Correlation {

	Long id
	String series1Type
	Long series1Id
	String series2Type
	Long series2Id
	Long userId
	Double corValue
	Double mipssValue
	Double overlapN
	Date created
	Date updated

	static constraints = {
		corValue nullable: true //, validator: { val, obj -> val && !Double.isNaN(val) }
		overlapN nullable: true
		mipssValue nullable: true
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

	def createOrUpdate
}
