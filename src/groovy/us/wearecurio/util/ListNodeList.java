package us.wearecurio.util;

/**
 *
 * @author mitsu
 */
public class ListNodeList<T extends ListNode> {
	protected T first;
	protected T last;
	
	public ListNodeList() {
		first = null;
		last = null;
	}
	
	public T getFirst() { return first; }
	
	public T getLast() { return last; }
	
	public T addFirst(T newFirst) {
		if (newFirst == null) return first;
		if (first == null) {
			first = newFirst;
			last = newFirst;
			
			return first;
		} else {
			first.prepend(newFirst);
			while (newFirst.getPrevious() != null)
				newFirst = (T) newFirst.getPrevious();
			
			first = newFirst;
		}
		
		while (newFirst.getPrevious() != null)
			newFirst = (T) newFirst.getPrevious();
		
		first = newFirst;
		return first;
	}
	
	public T addLast(T newLast) {
		if (newLast == null) return last;
		if (last == null) {
			first = newLast;
			last = newLast;
		} else {
			last.append(newLast);
			while (newLast.getNext() != null)
				newLast = (T) newLast.getNext();
			
			last = newLast;
			return last;
		}
		
		while (newLast.getNext() != null)
			newLast = (T) newLast.getNext();
		
		last = newLast;
		return last;
	}
	
	public T removeFirst() {
		T oldFirst = first;
		if (first == last) {
			first = null;
			last = null;
		} else if (first != null) {
			T newFirst = (T) first.getNext();
			first.remove();
			this.first = newFirst;
		}
		
		return oldFirst;
	}
	
	public T removeLast() {
		T oldLast = last;
		if (first == last) {
			first = null;
			last = null;
		} else if (last != null) {
			T newLast = (T) last.getPrevious();
			last.remove();
			this.last = newLast;
		}
		
		return oldLast;
	}
	
	public boolean isEmpty() {
		return first == null;
	}
}
