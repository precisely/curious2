package us.wearecurio.services

import java.util.Date;
import javax.persistence.TemporalType

import org.springframework.transaction.annotation.Transactional

import org.hibernate.StaleObjectStateException
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException
import java.lang.reflect.UndeclaredThrowableException

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
	public static retry(Object o, Closure c) throws Exception { // this is a general service method that may throw exceptions
		def retVal
		int retryCount = 0
		while (retryCount < 100) {
			try {
				Model.withTransaction { status ->
					retVal = c(status)
				}
				return retVal
			} catch (StaleObjectStateException e) {
				log.warn "Stale exception caught saving " + o
				if (++retryCount >= 3) { // if retry has failed three times, pause before reloading
					Thread.sleep(1000)
				}
				o.refresh()
			} catch (HibernateOptimisticLockingFailureException e2) {
				log.warn "Stale exception caught saving " + o
				if (++retryCount >= 3) { // if retry has failed three times, pause before reloading
					Thread.sleep(1000)
				}
				o.refresh()
			} catch (UndeclaredThrowableException e2) {
				// rethrow exceptions thrown inside transaction
				while (e2.getCause() instanceof UndeclaredThrowableException) {
					e2 = e2.getCause()
				}
				throw e2
			}
		}
		
		return null
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
