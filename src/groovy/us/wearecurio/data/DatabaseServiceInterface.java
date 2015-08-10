package us.wearecurio.data;

import java.util.List;
import java.util.Map;

public interface DatabaseServiceInterface {
	/**
	 * sqlRows
	 * 
	 * @param statement - sql statement with optional string identifiers
	 * @param args - map from string identifiers (referenced by :string) in sql statment to objects
	 * @return list of maps from name of return parameter to object return value, ie., longs, dates, strings
	 */
	List<Map<String, Object>> sqlRows(String statement, Map<String, Object> args);
}
