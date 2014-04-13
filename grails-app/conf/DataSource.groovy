dataSource {
    /*
    pooled = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
    */
	pooled = true
	//driverClassName = "org.hsqldb.jdbcDriver"
	//driverClassName = "org.postgresql.Driver"
	driverClassName = "com.mysql.jdbc.Driver"
	username = "curious"
	password = "734qf7q35"
	properties {
		testOnBorrow = true
		testWhileIdle = true
		testOnReturn = false
		validationQuery = "SELECT 1"
	}
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory'
}
// environment specific settings
environments {
	development {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb_dev"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
			//url = "jdbc:postgresql:tlb"
			//url = "jdbc:hsqldb:file:devDB;shutdown=true"
		}
	}
	test {
		dataSource {
			dbCreate = "create-drop" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb_test"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
		}
	}
	production {
		dataSource {
			dbCreate = "update" // one of 'create', 'create-drop','update'
			url = "jdbc:mysql://localhost/tlb"
			dialect = 'org.hibernate.dialect.MySQL5InnoDBDialect'
		}
	}
}
/*
environments {
    development {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:prodDb;MVCC=TRUE;LOCK_TIMEOUT=10000"
            pooled = true
            properties {
               maxActive = -1
               minEvictableIdleTimeMillis=1800000
               timeBetweenEvictionRunsMillis=1800000
               numTestsPerEvictionRun=3
               testOnBorrow=true
               testWhileIdle=true
               testOnReturn=true
               validationQuery="SELECT 1"
            }
        }
    }
}
*/