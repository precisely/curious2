package us.wearecurio.model

class Stats {
	
  // Sample mean
  public static mean(def x) {
	List <Double> values = x.clone()
	values.removeAll([null])
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
	  def mu = mean(values)
	  def dev = sd(values)
	  values.collect { it==null || mu== null || dev== null || dev==0 ? null : (it - mu)/ dev }
  }
  
  // Dot product.
  public static dot(x, y) {
	  [x, y].transpose().collect{ it[0] == null || it[1] == null ? null : it[0] * it[1] }
  }
  
  // Power.
  public static power(x, n) {
	  x.collect { it == null ? null : it ** n }
  }
  

  public static cor(values_x, values_y) {
	  def sdx = sd(values_x)
	  if (sdx == null || sdx == 0) { return null }
	  def sdy = sd(values_y)
	  if (sdy == null || sdy == 0) { return null }
	  
    def score_x = standardize(values_x)
    def score_y = standardize(values_y)
 
	  sum(dot(score_x, score_y)) / (sizeNotNull(score_x, score_y) - 1)
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
