package us.wearecurio.services

import us.wearecurio.utility.Utils
import us.wearecurio.model.Correlation
import us.wearecurio.model.CuriousSeries
import us.wearecurio.model.Stats
import us.wearecurio.model.Entry
import us.wearecurio.model.User

class CorrelationService {

	private static def LOG = new File("CorrelationService.out")
	def log(text) {
		LOG.withWriterAppend("UTF-8", { writer ->
			writer.write( "CorrelationService: ${text}\n")
		})
	}

	static transactional = false
	def entryService = new EntryService()
	def tagService = new TagService()

	// NB: CuriousSeries x and y can be generated from single tags or TagGroup
	//			 instances.  See the CuriousSeries contstructors for details.
	def correlate(CuriousSeries x, CuriousSeries y) {
		// Make sure they have the same start and end datetimes.
		if (x.start != y.start) {
			throw new Exception("CuriousSeries X has a different start time than series Y.")
		} else if (x.end != y.end) {
			throw new Exception("CuriousSeries X has a different end time than series Y.")
		} else if (x.size() != y.size()) {
			throw new Exception("CuriousSeries X has a different length than series Y.")
		}

		cor(x, y)
	}

	def correlateToChangesInY(CuriousSeries x, CuriousSeries y) {
		CuriousSeries edgeSeriesOfY = detectEdges(y)
		cor(x, edgeSeriesOfY)
	}

	public static def corDebug(user, tag1, tag2) {
		def series1 = CuriousSeries.create(tag1, user.id)
		def series2 = CuriousSeries.create(tag2, user.id)
		def corr = CuriousSeries.cor(series1, series2)
		def times = CuriousSeries.mergedTimes(series1, series2)
		def values1 = Stats.standardize(CuriousSeries.valuesOn(series1, times))
		def values2 = Stats.standardize(CuriousSeries.valuesOn(series2, times))
		def values3 = Stats.dot(values1, values2)
		println tag1.description
		println values1
		println tag2.description
		println values2
		println "merged:"
		println values3
		println "correlation:"

		def count = 0
		for (int i=0; i < values1.size(); i++) {
			if (values1[i] != null && values2[i] != null) {
				count += 1
				println "${i}: ${values1[i]} * ${values2[i]} = ${values3[i]}"
			}
		}
		println "---"
		println "sum1: ${Stats.sum(values1)}"
		println "sum2: ${Stats.sum(values2)}"
		println "sum_1,2: ${Stats.sum(values3)}"
		def cross_score = 0
		if (count > 0) {
			cross_score = Stats.sum(values3)/count
		}
		println "average cross-score (N=${count}): ${Stats.sum(values3)/count}"
		println corr
	}

	def cor(CuriousSeries x, CuriousSeries y) {
		CuriousSeries.cor(x, y)
	}

	def updateAllUserCorrelations() {
		new File("CorrelationService.out").delete()
		User.findAll().each { user ->
			updateUserCorrelations(user)
		}
	}

	def saveCorrelation(CuriousSeries series1, CuriousSeries series2) {
		def value = Stats.cor(series1.values, series2.values)
		def correlation = new Correlation(series1, series2, value)
		correlation.save()
	}

	def saveMipss(CuriousSeries series1, CuriousSeries series2) {
    def score = Correlation.findWhere(userId: series1.userId, series1Id: series1.sourceId, series2Id: series2.sourceId)
    if (score == null) {
		  score = new Correlation(series1, series2)
    }
		def result = CuriousSeries.mipss(series1, series2)

		if (result) {
			log("MIPSS: ${result['value']} (N=${result['N']}): (${series1.sourceId})${series1.sourceDescription} X (${series2.sourceId})${series2.sourceDescription}\n")
		}
		def mipss = null
		def N = null
		if (result && result['N'] && result['N'] > 0) {
			N = result['N']
			mipss = result['value']
		} else {
			return null
		}
		score.mipssValue = mipss
		score.overlapN = N
		score.save()
		result
	}

	def updateUserCorrelations(def user, startTime=null, stopTime=null) {
		startTime = startTime ?: entryService.userStartTime(user.id)
		stopTime = stopTime ?: entryService.userStopTime(user.id)
		def tags = Entry.findAllWhere('userId': user.id.toLong()).collect { it.tag }.unique()
		def all_series = tagsToSeries(tags, user)
		def score = null
		def N= null
		def mipss = null
		for (series1 in all_series) {
			for (series2 in all_series) {
				if (series1 != null && series2 != null && seriesOverlap(series1, series2)) {
					saveMipssWithLogging(series1, series2)
				} // if series overlap
			} // for
		} // for
	} // updateUserCorrelationsDebug

	def saveMipssWithLogging(series1, series2) {
		try {
			def result = saveMipss(series1, series2)
			if (result && result['N'] && result['N'] > 0 ) {
				log("MIPSS: ${result['value']} (N=${result['N']}): (${series1.sourceId})${series1.sourceDescription} X (${series2.sourceId})${series2.sourceDescription}\n")
			}
		} catch(e) {
			log("***** ERROR ${e.class}\n ${e.getMessage()}\n ${e.getStackTrace().join("\n")}:\n")
			log("series1 Details:\n")
			if (series1 != null) {
				log( "	${series1.sourceDescription} - ${series1.sourceId} - ${series1.values}\n")
			} else {
				log("null")
			}

			log("series2 Details:\n")
			if (series1 != null) {
				log( "	${series2.sourceDescription} - ${series2.sourceId} - ${series2.values}\n")
			} else {
				log("null")
			}
		} // try
	}


	def tagsToSeries(tags, user) {
		def all_series = []
		def new_series
		tags.each { tag ->
			try {
				new_series = CuriousSeries.create(tag, user.id)
				if (new_series == null) {
					log( "*NULL Series* ${tag.id} - ${tag.description}\n")
				} else {
					log( "*GOOD Series* ${tag.id} - ${tag.description}\n")
					all_series << new_series
				}
			} catch(e) {
				log("ERROR ${e.class}\n ${e.getMessage()}\n ${e.getStackTrace().join("\n")}:\n Tag Details: ${tag.id} - ${tag.description}\n")
			}
		}
		all_series
	}

	def seriesOverlap(series1, series2) {
		def series2_contains_an_endpoint_of_series1 = (series2.start_day() <= series1.start_day() && series1.start_day() <= series2.stop_day()) || (series2.start_day() <= series1.stop_day() && series1.stop_day() <= series2.stop_day())
		def series1_contains_both_end_points_of_series2 = (series1.start_day() < series2.start_day() && series2.stop_day() < series1.stop_day())
		series2_contains_an_endpoint_of_series1 || series1_contains_both_end_points_of_series2
	}

	def iterateOverTagPairs(user, f) {
		def tags = Entry.findAllWhere('userId': user.id.toLong()).collect { it.tag }.unique()
		//tag_groups = tagService.getTagGroupsByUser(user.id)
		def accum = []
		for (tag1 in tags) {
			for (tag2 in tags) {
				accum << f(tag1, tag2)
			}
		}
		accum
	}

	def iterateOverTagPairsAsSeries(user, user_defined_cb) {
		def series_cb = { tag1, tag2 ->
			def series1 = CuriousSeries.create(tag1, user.id)
			def series2 = CuriousSeries.create(tag2, user.id)
			user_defined_cb(series1, series2)
		}
		iterateOverTagPairs(user, series_cb)
	}
}
