package us.wearecurio.data

import groovy.sql.Sql

class MySQLDatabaseService implements DatabaseServiceInterface {
	Sql sql
	
	MySQLDatabaseService(String hostName, String databaseName, String userName, String password) {
		sql = Sql.newInstance("jdbc:mysql://" + hostName + "/" + databaseName, userName,
			password, "com.mysql.jdbc.Driver")
	}
	
	@Override
	public List<Map<String, Object>> sqlRows(String sqlString,
			Map<String, Object> args) {
		return sql.rows(sqlString, args)
	}
}
