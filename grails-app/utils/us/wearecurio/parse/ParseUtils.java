package us.wearecurio.parse;

import java.util.LinkedList;

public class ParseUtils {
	public static String implode(final LinkedList<String> words) {
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		
		for (String word : words) {
			if (!first) {
				buf.append(" ");
			} else
				first = false;						
			
			buf.append(word);
		}
		
		return buf.toString();
	}
	
	public static String implode(final LinkedList<String> words, final String separator) {
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		
		for (String word : words) {
			if (!first) {
				buf.append(separator);
			} else
				first = false;						
			
			buf.append(word);
		}
		
		return buf.toString();
	}
	
	public static String implode(final LinkedList<String> words, int maxWords) {
		return implode(words, " ", maxWords);
	}
	
	public static String implode(final LinkedList<String> words, final String separator, int maxWords) {
		int i = 0;
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		
		for (String word : words) {
			if (++i > maxWords)
				break;
			
			if (!first) {
				buf.append(separator);
			} else
				first = false;						
			
			buf.append(word);
		}
		
		return buf.toString();
	}
}
