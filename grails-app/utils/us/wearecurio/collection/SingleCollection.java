package us.wearecurio.collection;

import java.util.Collection;
import java.util.Iterator;

public class SingleCollection<T> implements Collection<T> {
	private T contents;
	
	public SingleCollection(T obj) {
		contents = obj;
	}
	
	public T getContents() {
		return contents;
	}

	@Override
	public boolean add(T e) {
		contents = e;
		
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		for (T element : c) {
			contents = element;
			return true;
		}
		
		return true;
	}

	@Override
	public void clear() {
		contents = null;
	}

	@Override
	public boolean contains(Object o) {
		return contents == o;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return contents != null;
	}

	@Override
	public Iterator<T> iterator() {
		return new SingleIterator<T>(this);
	}

	@Override
	public boolean remove(Object o) {
		if (contents == o) {
			contents = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return false;
	}

	@Override
	public int size() {
		return contents == null ? 0 : 1;
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return null;
	}
}
