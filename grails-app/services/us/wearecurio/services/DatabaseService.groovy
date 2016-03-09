package us.wearecurio.services

import org.apache.commons.logging.LogFactory
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.StaleObjectStateException
import org.hibernate.transform.AliasToEntityMapResultTransformer
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException
import us.wearecurio.data.DatabaseServiceInterface
import us.wearecurio.model.Model

import java.lang.reflect.UndeclaredThrowableException

class DatabaseService implements DatabaseServiceInterface {

	private static def log = LogFactory.getLog(this)

	static transactional = false
	
	static final long TEST_MIGRATION_ID = 30L
	
	static def service
	
	static def set(s) { service = s }

	static DatabaseService get() { return service }
		
	SessionFactory sessionFactory
	
	// must be called from outside a transaction
	static retry(Object o, Closure c) throws Exception { // this is a general service method that may throw exceptions
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
			} catch (UndeclaredThrowableException e3) {
				def e = e3
				// rethrow exceptions thrown inside transaction
				while (e instanceof UndeclaredThrowableException) {
					if (e.getCause() != null) {
						e = e.getCause()
					} else if (e.getUndeclaredThrowable() != null) {
						e = e.getUndeclaredThrowable()
					} else
						break
				}
				if (e != null) {
					throw e
				}
			}
		}
		
		return null
	}
	
	Query sqlQuery(String statement, def args = []) {
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
	
	def sql(String statement) {
		sqlQuery(statement).executeUpdate()
	}
	
	boolean sqlNoRollback(String statement, args = []) {
		try {
			sqlQuery(statement, args).executeUpdate()
		} catch (RuntimeException e) {
			e.printStackTrace()
			return false
		}
	
		return true
	}
	
	@Override
	List<Map<String, Object>> sqlRows(String statement, Map<String, Object> args) {
		return sqlQuery(statement, args).list()
	}

	List<Map<String, Object>> sqlRows(String statement) {
		return sqlQuery(statement, []).list()
	}
	
	def eachRow(String statement, Closure c) {
		sqlQuery(statement).list().each(c)
	}
}
