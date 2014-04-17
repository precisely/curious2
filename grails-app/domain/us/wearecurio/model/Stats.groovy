package us.wearecurio.model

class Stats {

	// Sample mean
	public static mean(def x) {
		List <Double> values = x.clone()
		values.removeAll([null])
		if (x.size() < 2) {
			return x.first()
		}
		values.sum()/values.size()
	}

	// Sample standard deviation
	public static sd(def x) {
		List <Double> values = x.clone()
		values.removeAll([null])
		if (values.size() < 2) {
			return null
		}
		def mu = mean(values)
		def numerator = values.collect { (it - mu)**2 }.sum()
		def denominator  = values.size() - 1
		Math.sqrt( numerator / denominator )
	}

	// A standardized random variable (subtract mean and divide by standard deviation).
	public static standardize(def values) {
		// We want to be able to standardize a list of numbers even if it doesn't
		//	 a meaningful standard deviation.  For example,

		// had fun 10
		// had fun 10
		// had fun 10

		// We don't want to skip this sequence of events just because we don't know
		//	 how to compute the standard deviation.
		// In this case, let's just divide by the mean to standardize it.  So, it
		//	 just becomes a sequence of ones.
		// Another case to consider is when you only have 1 data point.  Again, we
		//	 we don't want to discard the event just because we can't compute a
		//	 so again just replace it with a one.
		def mu = mean(values)
		def numerator_of_deviation_is_zero = values.collect { it==null ? 0.0 : (it - mu)**2  }.sum() == 0
		def event_like = numerator_of_deviation_is_zero || sizeNotNull(values) == 1
		values.collect {
			if (it==null || mu==null) {
				null
			} else if (event_like) {
				1
			} else {
				(it - mu)/ sd(values)
			}
		}
	}

	// Dot product.
	public static dot(x, y) {
		[x, y].transpose().collect{ it[0] == null || it[1] == null ? null : it[0] * it[1] }
	}

	// Power.
	public static power(x, n) {
		x.collect { it == null ? null : it ** n }
	}


	// MSS stands for "mean standardized score."
	public static mipss(values_x, values_y) {
		if (sd(values_x) == null) { return null }
		if (sd(values_y) == null) { return null }

		def mss_x = standardize(values_x)
		def mss_y = standardize(values_y)

		def N = 0
		Double the_sum = 0
		def size = mss_x.size()
		for (int i=0; i < size; i++) {
			if (mss_x[i] != null && mss_y[i] != null) {
				N += 1
				the_sum += mss_x[i] * mss_y[i]
			}
		}
		if (Double.isNaN(the_sum/N)) {
			return null
		} else {
			the_sum /= N
		}

		['value': the_sum, 'N': N]
	}

	public static cor(values_x, values_y) {
		// Pearson Correlation coefficient needs a valid nonzero
		//	 standard deviation.	And you need at least 2 data points.
		def N = values_x.size() - 1
		def sdx = sd(values_x)
		if (N < 1 || sdx == null || sdx == 0) { return null }
		def sdy = sd(values_y)
		if (sdy == null || sdy == 0) { return null }

		def mss_x = standardize(values_x)
		def mss_y = standardize(values_y)

		sum(dot(mss_x, mss_y)) / N
	}

	// The number of non-null values in a vector.
	public static sizeNotNull(x) {
		List <Double> z = x.clone()
		z.removeAll([null])
		z.size()
	}

	// The number of non-null values in the same time slot between two vectors.
	public static sizeNotNull(x, y) {
		List <Double> z = dot(x, y).clone()
		z.removeAll([null])
		z.size()
	}

	// A version of sum that is resilient to null values.
	public static sum(x) {
		List <Double> values = x.clone()
		values.removeAll([null])
		values.sum()
	}
}
