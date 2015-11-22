package us.wearecurio.services

class SearchQueryService {
	static String normalizeQuery(String query) {
		if (!query || query == "") {
			return ""
		}
		
		if (query.trim().matches(/^".*"$/)) {
			return  query.replaceAll(/([\+-=><!&\|\(\)\{\}\[\]\^~\*\?:\/])/,$/\\$1/$)
		}
		
		def tokens = query.replaceAll(/([\+-=><!&\|"\(\)\{\}\[\]\^~\*\?:\/])/,$/\\$1/$).split().toUnique()
		def normalizedTokens = []
		tokens.each {
			if (it.matches($/(^[oO][rR]$|^[aA][nN][dD]$)/$)) {
				def andOrNot = $/(^[oO][rR]$|^[aA][nN][dD]$|^[nN][oO][tT]$)/$
				normalizedTokens << "(${it.replaceAll(andOrNot, $/"$1"*/$)} OR ${it.replaceAll(andOrNot, $/#$1*/$)})"
			} else if (it.matches($/(^#.*$)/$)){
				if (!tokens.contains(it.substring(1))) {
					normalizedTokens << /${"$it*"}/
				}
			} else {
				normalizedTokens << /(${"$it*"} OR ${"#$it*"})/
			}
		}
	
		String ret = normalizedTokens.join(" AND ")
		
		if (normalizedTokens.size == 1) {
			return ret
		} else {
			return "($ret)"
		}	
	}
}
