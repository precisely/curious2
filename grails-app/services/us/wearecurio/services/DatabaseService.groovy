package us.wearecurio.services

import java.util.Date;

import org.apache.commons.logging.LogFactory
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.transform.AliasToEntityMapResultTransformer

import us.wearecurio.server.Migration;
import us.wearecurio.utility.Utils;
import us.wearecurio.model.*;
import grails.util.Environment

import groovy.sql.Sql

class DatabaseService {

	private static def log = LogFactory.getLog(this)

	static transactional = true
	
	public static final long TEST_MIGRATION_ID = 30L
	
	static def service
	
	public static def set(s) { service = s }

	public static DatabaseService get() { return service }
		
	SessionFactory sessionFactory
	
	public Query sqlQuery(String statement) {
		Session session = sessionFactory.getCurrentSession()
		Query query = session.createSQLQuery(statement)
		query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
		return query
	}
	
	public Query resultSqlQuery(String statement) {
		Session session = sessionFactory.getCurrentSession()
		Query query = session.createSQLQuery(statement)
		query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
		return query
	}
	
	public def sql(String statement) {
		sqlQuery(statement).executeUpdate()
	}
	
	public def sqlRows(String statement) {
		return resultSqlQuery(statement).list()
	}

	public def eachRow(String statement, Closure c) {
		resultSqlQuery(statement).iterator().each(c)
	}
}
