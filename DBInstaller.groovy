import groovy.sql.Sql


/**
 * The class DBInstaller installs/updates the database using the install/update SQL scripts,
 * based on the current database condition and version.
 *  
 * @author janesh
 * @version 2.0.0
 * @since 2.0.0
 * 
 * NOTE: The groovy script uses the filename as the class name. Hence class not created explicitly.
 */
 
 		// invoke the install method.
		properties.put("db.name", args[0])
		properties.put("mysql.url", "jdbc:mysql://${properties["${properties['db.name']}"+'.host']}:${properties["${properties['db.name']}"+'.port']}/mysql")
		install()

		/* The install process */
		def install() {
			println "Started DB install/update process"
			init()
			boolean dbExists = checkDatabaseExists()
			if (dbExists) {
					println "Database already exists"
					boolean newVersion = isNewVersion()
					if (newVersion) {
						println "Going to update the database as new version was found"
						updateInstall()
						updateDatabaseVersion()
					} else {
						println "Database is up-to-date. Nothing to do"
						// System.exit(0);
					}
				} else {
					println "DB does not exist. Going to do fresh install"
					freshInstall()
					insertDatabaseVersion()
				}
			println "Completed DB install/update process"
		}
		
		/* Initialize the process */
		def init() {
			println "initializing the db install process: ${properties['mysql.url']}"
			sql = Sql.newInstance("${properties['mysql.url']}", "${properties["${properties['db.name']}"+'.root.username']}", "${properties["${properties['db.name']}"+'.root.password']}", "${properties['driver']}")
			println 'initialization complete'
		}	
			
		/* Check if database exists */
		boolean checkDatabaseExists() {
			println 'Checking if database exists'
			boolean dbExists;
			/* TODO: Get DB name dynamically */
			sql.query("select * from information_schema.schemata where lower(schema_name) = ${properties['db.name']}") { rs ->
				if(rs.next()){
					// if the resultset is non-empty, the database is available.
					dbExists = true
				} else {
					dbExists = false
				}
			}
			return dbExists
		}
		
		/* Get version number from properties file */
		int getPropertiesVersion() {
			String versionProp = "${properties['db.name']}.version"
			String propVer = "${properties[versionProp]}"
			int propVersion = Integer.parseInt(propVer)
			println 'version number in properties file is '+propVersion
			return propVersion
		}
		
		/* Get version number from database */
		int getDatabaseVersion() {
			int dbVersion = 0
			sql.query("select * from information_schema.tables where table_schema=${properties['db.name']} and table_name ='version_info'"){ resultSet ->
				if(resultSet.next()){
					/* TODO: Get DB name dynamically */
					String query = "select * from ${properties['db.name']}.version_info"
					sql.query(query){ resultSet1 ->
						if(resultSet1.next()){
							dbVersion = resultSet1.getInt('version')
							println 'version number of database is '+dbVersion
						} else {
							/*The database has an empty version_info table*/
							String insertStmt = "insert into ${properties['db.name']}.version_info (version) values(1)"
							sql.execute(insertStmt)
							dbVersion = 1
							println 'database has empty version_info table. Hence assuming version number to be 1'
						}
					}
				} else {
					throw new Exception('The existing database seems to be an old database created without this installer and hence it is incompatible with this installer. Please drop the database and rerun this script.');
				}
			}
			return dbVersion
		}
			
		/* Check if the properties file has a newer version */
		boolean isNewVersion() {
			println 'Checking if new version'
			boolean isNewVer
			int propVersion = getPropertiesVersion()
			int dbVersion = getDatabaseVersion()
			if (propVersion > dbVersion) 
				{
					isNewVer = true
				} else {
					isNewVer = false
				}
			return isNewVer
		}
		
		/* Update the version information in the database */
		def updateDatabaseVersion() {
			int propVersion = getPropertiesVersion()
			println 'Update database with version number '+propVersion
			/* TODO: Get DB name dynamically */
			String query = "update ${properties['db.name']}.version_info set version = "+propVersion
			sql.execute(query)
		}
	
		/* Insert the version information in the database - for fresh install */
		def insertDatabaseVersion() {
			int propVersion = getPropertiesVersion()
			println 'Inserting into database the version number '+propVersion
			/* TODO: Get DB name dynamically */
			String query = "insert into ${properties['db.name']}.version_info (version) values("+propVersion+")"
			sql.execute(query)
		}
		
		def freshInstall() {
			println 'Started fresh install of database'

			/* Running the create users and grant priviliges script separately - as there
                                would be errors when trying to create users who already exist. 'onerror' attribute here
                                would continue execution even on encountering such errors.  */
			ant.sql(driver:"${properties['driver']}", url:"${properties['mysql.url']}",userid:"${properties["${properties['db.name']}"+'.root.username']}", 		password:"${properties["${properties['db.name']}"+'.root.password']}", src:"${properties['sql_dir']}/${properties['db.name']}_prereq.sql", onerror:"continue", classpath:"${properties['db.install.classpath']}")
			
			/* TODO: The file ${db.name}_update_seed.sql contains the database patch/update */
			/* NOTE: Semicolon in c-style multiline comments in SQL files will fail: refer ANT SQL task bug id 41737 */
			for (String sqlFile in ["${properties['sql_dir']}/${properties['db.name']}_objects.sql", 
			"${properties['sql_dir']}/${properties['db.name']}_users_seed.sql", 
			"${properties['sql_dir']}/${properties['db.name']}_seed.sql"]) {
				def file=new java.io.File(sqlFile)
				if(file.exists()){
					ant.sql(driver:"${properties['driver']}", url:"${properties['mysql.url']}", userid:"${properties["${properties['db.name']}"+'.root.username']}", password:"${properties["${properties['db.name']}"+'.root.password']}", src:sqlFile, classpath:"${properties['db.install.classpath']}")
				} else{
					println 'SQLScript '+sqlFile+' not Found'
				}
			}
			installProcedures()
			println 'Completed fresh install of database'
		}

		def updateInstall() {
			println 'Started update of database'
			// dbBackup()
			for (String sqlFile in ["${properties['sql_dir']}/${properties['db.name']}_update_seed.sql"]) 
			{
				def file=new java.io.File(sqlFile)
				if(file.exists()){
					ant.sql(driver:"${properties['driver']}", url:"${properties['mysql.url']}", userid:"${properties["${properties['db.name']}"+'.root.username']}", password:"${properties["${properties['db.name']}"+'.root.password']}", src:sqlFile, delimiter:"\$\$", classpath:"${properties['db.install.classpath']}") 
				} else{
					println 'SQLScript '+sqlFile+' not Found'
				}
			}
			installProcedures()
			println 'Completed update of database'
		}

		def installProcedures() {
			println 'Started installation of procedures'
			/* The procedure fails on being invoked using sql task. So using mysql command based execution. */
			def sqlFile="${properties['sql_dir']}/${properties['db.name']}_procedures.sql"
			def file=new java.io.File(sqlFile)
			if(file.exists()){
				ant.sql(driver:"${properties['driver']}", url:"${properties["${properties['db.name']}"+'.url']}", userid:"${properties["${properties['db.name']}"+'.root.username']}", password:"${properties["${properties['db.name']}"+'.root.password']}", src:sqlFile, delimiter:"\$\$", classpath:"${properties['db.install.classpath']}")
			} else{
				println 'SQLScript '+sqlFile+' not Found'
			}
                	println 'Completed installation of procedures'
	       }
	       
	        def dbBackup() {
	                def dbVersion = getDatabaseVersion();
	                def fileName = "${properties['db.name']}_backup_"+dbVersion+".sql";
	                println "Taking DB backup of ${properties['db.name']} to ${properties['install.root.dir']}/dbBackup/"+fileName
	                ant.exec(failonerror:"false",output:"${properties['install.root.dir']}/dbBackup/"+fileName,executable:"mysqldump") {
	                	arg(line:"-u${properties["${properties['db.name']}"+'.root.username']} -p${properties["${properties['db.name']}"+'.root.password']} -h${properties["${properties['db.name']}"+'.host']} --routines -B ${properties['db.name']}")
	                }
	                def stdErr = properties['ant.project.properties.cmdErr']
	                if(stdErr != null) {
	                	println "Error in taking backup, mysql might not be present in the installation box"
	                        println 'STDERR: '+stdErr
	                }
	        }


		def setProperties(Map properties) {
			this.properties = properties;
		}
		
		def setAnt(AntBuilder ant) {
			this.ant = ant;
		}
