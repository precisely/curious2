package us.wearecurio.factories

import us.wearecurio.model.Tag


class TagFactory {
	
  public static def make(tagDescription='tag1') {
    if (tagDescription == null) {
      tagDescription = 'tag1'
    }
	  def tag = Tag.findWhere(description: tagDescription)
	  if (!tag) {
		  tag = Tag.create(tagDescription).save() 
	  }
	  tag
  }
  
  public static def makeN(long n) {
	  def arr = (1..n).toArray()
	  return arr.collect { TagFactory.make("tag" + it) }
  }
  
}
