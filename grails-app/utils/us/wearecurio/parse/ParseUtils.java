package us.wearecurio.parse;

import java.util.LinkedList;

public class ParseUtils {
	public static String implode(final LinkedList<String> words, final String separator) {
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		
		for (String word : words) {
			if (!first) buf.append(separator);
			
			buf.append(word);
		}
		
		return buf.toString();
	}
	
	public static String implode(final LinkedList<String> words, final String separator, int maxWords) {
		int i = 0;
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		
		for (String word : words) {
			if (++i > maxWords)
				break;
			
			if (!first) buf.append(separator);
			
			buf.append(word);
		}
		
		return buf.toString();
	}
}
