package us.wearecurio.model

class Correlation {
  
	Long id
	def series1Source
	Long series1Id
	def series2Source
	Long series2Id
	Double corValue  
	Date created
	Date updated

	static constraints = {
		corValue nullable: true
	}

	Correlation(Series series1, Series series2, Double corValue) {
		series1Source = series1.source.toString()
		series2Source = series2.source.toString()
		series1Id = series1.sourceId
		series2Id = series2.sourceId
		
        this.corValue = corValue
		if (Double.NaN == corValue) {
			this.corValue = null
		}
		created = new Date()
		updated = new Date()
	}
}
