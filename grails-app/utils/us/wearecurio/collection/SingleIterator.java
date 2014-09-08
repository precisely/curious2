package us.wearecurio.collection;

import java.util.Iterator;

public class SingleIterator<T> implements Iterator<T> {
	private T contents;
	private boolean initial;
	
	public SingleIterator(SingleCollection<T> c) {
		contents = c.getContents();
		initial = true;
	}

	@Override
	public boolean hasNext() {
		return initial;
	}

	@Override
	public T next() {
		if (initial) {
			initial = false;
			return contents;
		} else
			return null;
	}

	@Override
	public void remove() {
		contents = null;
	}
}
