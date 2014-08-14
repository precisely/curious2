package us.wearecurio.parse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * Matches from an iterator of word strings to a target object
 * 
 * Initialized with a list of words to match, and target object at the end of the list
 * 
 * @author mitsu
 */
public class WordMatcher<T> {
	Map<String, WordMatcher<T>> map = new HashMap<String, WordMatcher<T>>();
	T value;
	
	public WordMatcher() {
	}
	
	public WordMatcher<T> addAll(Map<ArrayList<String>, T> initMap) {
		for (Map.Entry<ArrayList<String>, T> entry : initMap.entrySet()) {
			add(entry.getKey().iterator(), entry.getValue());
		}
		
		return this;
	}
	
	public void add(Iterator<String> words, T value) {
		if (words.hasNext()) {
			String word = words.next();
			addWordMatcher(word).add(words, value);
		} else
			this.value = value;
	}
	
	protected WordMatcher<T> addWordMatcher(String word) {
		WordMatcher<T> matcher = map.get(word);
		
		if (matcher == null) {
			matcher = new WordMatcher<T>();
			map.put(word, matcher);
			return matcher;
		}
		
		return matcher;
	}
	
	protected WordMatcher<T> matchWord(String word) {
		return map.get(word);
	}
	
	public T matchRemove(LinkedList<String> words, int offset) {
		int len = 0;
		ListIterator<String> wordIterator = words.listIterator(offset);
		
		WordMatcher<T> current = this;
		
		while (true) {
			if (!wordIterator.hasNext()) {
				// successful match, no more words
				// remove matching words from input list
				while (len-- > 0) {
					words.remove(offset);
				}
				return current.getValue();
			}
			
			String word = wordIterator.next();
			
			++len;
			
			WordMatcher<T> next = current.matchWord(word);
			if (next == null) { // no more matches, return value
				T value = current.getValue();
				if (value != null)
					while (len-- > 0) {
						words.remove(offset);
					}

				return value;
			}
			current = next;
		}
	}
	
	public T match(LinkedList<String> words, int offset) {
		ListIterator<String> wordIterator = words.listIterator(offset);
		
		WordMatcher<T> current = this;
		
		while (true) {
			if (!wordIterator.hasNext()) {
				return current.getValue();
			}

			String word = wordIterator.next();

			WordMatcher<T> next = current.matchWord(word);
			if (next == null) { // no more matches, return value
				return current.getValue();
			}
			current = next;
		}
	}
	
	public T getValue() {
		return value;
	}
}
