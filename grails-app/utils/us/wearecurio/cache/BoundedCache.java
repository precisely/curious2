package us.wearecurio.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public class BoundedCache<Key, Value> extends LinkedHashMap<Key, Value> {
	private static final long serialVersionUID = -8693356386725827354L;

	private static final int DEFAULT_INITIAL_CAPACITY = 1000;
 
    private static final float DEFAULT_LOAD_FACTOR = (float) 0.75;
 
    private final int bound;
 
	public BoundedCache() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}
	
	public BoundedCache(int bound) {
		this(bound, DEFAULT_LOAD_FACTOR);
	}
	
    public BoundedCache(int bound, float loadFactor) {
        super(bound, loadFactor, true);
        this.bound = bound;
    }
 
    @Override
    protected boolean removeEldestEntry(Map.Entry<Key, Value> eldest) {
        return size() > bound;
    }
}
