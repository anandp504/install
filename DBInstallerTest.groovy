import java.util.Map;
import java.util.HashMap;

/**
 * Test class for DBInstaller 
 *
 * @author janesh
 * @version 1.0.0
 */

class DBInstallerTest extends GroovyTestCase {
   private installer
   private Map properties
   private String nameOfDB = "ta_system_db"
   private String sqlDir = "/opt/p4/depot/Tumri/tas/ta/sql";
   void setUp() {
	installer = new DBInstaller()
   }

   private void initProperties() {
     	properties = new HashMap();
	properties.put("sql_dir", sqlDir);
     	properties.put("db.name", nameOfDB);
     	properties.put("mysql.url", "jdbc:mysql://localhost:3306/mysql")
	properties.put(nameOfDB+".version", "7");
	properties.put(nameOfDB+".host", "localhost");
	properties.put(nameOfDB+".port", "3306");
	properties.put(nameOfDB+".url", "jdbc:mysql://localhost:3306/"+nameOfDB);
	properties.put(nameOfDB+".root.username", "root");
	properties.put(nameOfDB+".root.password", "root");
	properties.put(nameOfDB+".username", "root");
	properties.put(nameOfDB+".password", "root");
	properties.put("driver", "com.mysql.jdbc.Driver");

	properties.put("db.host", "localhost");
	properties.put("db.port", "3306");
	properties.put("url", "jdbc:mysql://localhost:3306/"+nameOfDB);
	properties.put("db.root.username", "root");
	properties.put("db.root.password", "root");
	properties.put("username", "TAUSER");
	properties.put("password", "w3lc0m31");
   }

   void testInstall() {
	initProperties();
	installer.setProperties(properties);
	installer.setAnt(new AntBuilder());
     	installer.install();
    	// assertEquals("Groovy should add correctly", 2, 1 + 1)
	println "Test Successful"
   }
}

