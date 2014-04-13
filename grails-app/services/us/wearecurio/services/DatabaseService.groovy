package us.wearecurio.services

import java.util.Date;
import javax.persistence.TemporalType

import org.springframework.transaction.annotation.Transactional

import org.hibernate.StaleObjectStateException

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

	static transactional = false
	
	public static final long TEST_MIGRATION_ID = 30L
	
	static def service
	
	public static def set(s) { service = s }

	public static DatabaseService get() { return service }
		
	SessionFactory sessionFactory
	
	// must be called from outside a transaction
	public static retry(Object o, Closure c) {
		int retryCount = 0
		while (retryCount < 100) {
			if (retryCount > 3) {
				Thread.sleep(1000) // pause for a second before retrying
			}
			try {
				Model.withTransaction { status ->
					c(status)
				}
				return true
			} catch (StaleObjectStateException e) {
				log.warn "Stale exception caught saving " + o
				o.refresh()
				++retryCount
			}
		}
		
		return false
	}
	
	public Query sqlQuery(String statement, def args = []) {
		Session session = sessionFactory.getCurrentSession()
		Query query = session.createSQLQuery(statement)
		if (args instanceof Map) {
			for (e in args) {
				def key = e.key
				def value = e.value
				if (value instanceof List)
					query.setParameterList(key, value)
				else
					query.setParameter(key, value)
			}
		} else {
			int l = args.size()
			for (int i = 0; i < l; ++i) {
				def arg = args[i]
				query.setParameter(i, arg)
			}
		}
		query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE)
		return query
	}
	
	public def sql(String statement) {
		sqlQuery(statement).executeUpdate()
	}
	
	public boolean sqlNoRollback(String statement, args = []) {
		try {
			sqlQuery(statement, args).executeUpdate()
		} catch (RuntimeException e) {
			e.printStackTrace()
			return false
		}
	
		return true
	}
	
	public def sqlRows(String statement, args = []) {
		return sqlQuery(statement, args).list()
	}

	public def eachRow(String statement, Closure c) {
		sqlQuery(statement).list().each(c)
	}
}
