package us.wearecurio.model

class Correlation {

	Long id
	def series1Type
	Long series1Id
	def series2Type
	Long series2Id
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

	static mapping = {
		series1Type defaultValue: 'Tag'
		series2Type defaultValue: 'Tag'
	}

	public Correlation(CuriousSeries series1, CuriousSeries series2) {
		series1Type = series1.source.toString()
		series2Type = series2.source.toString()
		series1Id = series1.sourceId
		series2Id = series2.sourceId
		created = new Date()
		updated = new Date()
	}

	public Correlation(CuriousSeries series1, CuriousSeries series2, Double corValue) {
		series1Type = series1.source.toString()
		series2Type = series2.source.toString()
		series1Id = series1.sourceId
		series2Id = series2.sourceId

		this.corValue = corValue
		if (Double.NaN == corValue) {
			this.corValue = null
		}
		created = new Date()
		updated = new Date()
	}

	def setCorValue(value) {
		if (value && Double.isNan(value)) {
			this.value = value
		}
	}
}
