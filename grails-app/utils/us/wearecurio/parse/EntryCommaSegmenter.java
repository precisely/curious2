package us.wearecurio.parse;

public class EntryCommaSegmenter {
	String text;
	int offset;
	
	public EntryCommaSegmenter(String text) {
		this.text = text;
		this.offset = 0;
	}
	
	public boolean hasNext() {
		return this.offset >= 0 && this.offset < this.text.length();
	}
	
	public String next() {
		if (this.offset < 0 || this.offset >= this.text.length()) { // no more text
			return null;
		}
		
		int nextOffset = offset;
		
		while (true) {
			int nextComma = text.indexOf(',', nextOffset);
			if (nextComma == -1) {
				String retVal = text.substring(offset).trim();
				this.offset = -1;
				return retVal;
			}
			
			int nextParen = text.indexOf('(', nextOffset);
			
			if (nextParen == -1 || nextParen > nextComma) {
				String retVal = text.substring(offset, nextComma).trim();
				this.offset = nextComma + 1;
				return retVal;
			}
			
			int endParen = this.endParen(nextParen);
			if (endParen == -1) {
				String retVal = text.substring(offset).trim();
				this.offset = -1;
				return retVal;
			}
			nextOffset = endParen + 1;
		}
	}
	
	protected int endParen(int startParen) {
		while (true) {
			int nextEndParen = text.indexOf(')', startParen + 1);
			if (nextEndParen == -1) {
				return -1;
			}
	
			startParen = text.indexOf('(', startParen + 1);
			if ((startParen == -1) || (startParen > nextEndParen)) {
				return nextEndParen;
			}
			
			startParen = endParen(startParen);
			if (startParen == -1) {
				return -1;
			}
		}
	}
}
