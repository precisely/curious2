package us.wearecurio.util;

/**
 *
 * @author mitsu
 */
public class ListNode {
	protected ListNode previous;
	protected ListNode next;
	
	public ListNode() {
		previous = null;
		next = null;
	}
	
	public ListNode getPrevious() { return previous; }
	
	public ListNode getNext() { return next; }
	
	public ListNode prepend(ListNode previous) {
		this.previous = previous;
		if (previous != null)
			previous.next = this;
		
		return this;
	}
	
	public ListNode append(ListNode next) {
		this.next = next;
		if (next != null)
			next.previous = this;
		
		return this;
	}
	
	public ListNode removeNext() {
		ListNode oldNext = this.next;
		if (oldNext != null) {
			oldNext.previous = null;
			this.next = null;
		}
		
		return oldNext;
	}
	
	public ListNode remove() {
		if (previous != null) {
			previous.removeNext();
		}
		if (next != null) {
			removeNext();
		}
		
		return this;
	}
	
	public ListNode setPreviousNode(ListNode newPrevious) {
		this.previous = newPrevious;
		
		return this;
	}
	
	public ListNode setNextNode(ListNode newNext) {
		this.next = newNext;
		
		return this;
	}
}
