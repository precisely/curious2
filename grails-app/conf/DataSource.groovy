dataSource {
	/*
		pooled = true
		jmxExport = true
		driverClassName = "org.h2.Driver"
		username = "sa"
		password = ""
		*/
	pooled = true
	//driverClassName = "org.hsqldb.jdbcDriver"
	//driverClassName = "org.postgresql.Driver"
	driverClassName = "com.mysql.jdbc.Driver"
	// IMPORTANT: If you edit these credentials, you must also edit src/clojure/analytics/profiles.clj
	username = "curious"
	password = "734qf7q35"
	properties {
		testOnBorrow = true
		testWhileIdle = true
		testOnReturn = false
		validationQuery = "SELECT 1"
		minEvictableIdleTimeMillis = 60000 * 10
		timeBetweenEvictionRunsMillis = 60000 * 10
	}
}
hibernate {
		cache.use_second_level_cache = true
		cache.use_query_cache = false
//		cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
		cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
		singleSession = true // configure OSIV singleSession mode
		flush.mode = 'manual' // OSIV session flush mode outside of transactional context
}

// environment specific settings
environments {
	development {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb_dev?zeroDateTimeBehavior=convertToNull"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
			//url = "jdbc:postgresql:tlb"
			//url = "jdbc:hsqldb:file:devDB;shutdown=true"
			//loggingSql = true
		}
	}
	test {
		dataSource {
			dbCreate = "create-drop" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb_test?zeroDateTimeBehavior=convertToNull"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
		}
	}
	qa {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb?zeroDateTimeBehavior=convertToNull"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
		}
	}
	production {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb?zeroDateTimeBehavior=convertToNull"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
		}
	}
	/*
		development {
				dataSource {
						dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
						url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
				}
		}
		test {
				dataSource {
						dbCreate = "update"
						url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
				}
		}
		production {
				dataSource {
						dbCreate = "update"
						url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
						properties {
							 // See http://grails.org/doc/latest/guide/conf.html#dataSource for documentation
							 jmxEnabled = true
							 initialSize = 5
							 maxActive = 50
							 minIdle = 5
							 maxIdle = 25
							 maxWait = 10000
							 maxAge = 10 * 60000
							 timeBetweenEvictionRunsMillis = 5000
							 minEvictableIdleTimeMillis = 60000
							 validationQuery = "SELECT 1"
							 validationQueryTimeout = 3
							 validationInterval = 15000
							 testOnBorrow = true
							 testWhileIdle = true
							 testOnReturn = false
							 jdbcInterceptors = "ConnectionState"
							 defaultTransactionIsolation = java.sql.Connection.TRANSACTION_READ_COMMITTED
						}
				}
		}
		*/
}
