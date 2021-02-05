package kb.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigsLoader {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigsLoader.class.getName());
	
	private static ConfigsLoader configLoader;
	String environment;
	String graphdb;
	String bugPredictorServer;
	String ansiblePath;
	String reasonerServer;
	
	String kbUsername;
	String kbPassword;
	
	String keycloak;
	String keycloakClientId;
	String keycloakClientSecret;
	
	//envs for authentication
	//public static final List<String> AUTHENVS = Collections.unmodifiableList(
	//	    Arrays.asList(new String[] {""}));
		
	private ConfigsLoader(){}

	public static ConfigsLoader getInstance(){
		if (configLoader == null) {
			configLoader = new ConfigsLoader();
		}

		return configLoader;
	}
	
	/**
	* Function that loads the config.properties file.
	*/
	public void  loadProperties() {
		// Load properties file
		Properties properties = new Properties();
	
		environment =  System.getenv("environment");
		LOG.info("environment = {}", environment);

		if (environment == null) {
			environment = "dev";

			String configPath = "/envs/" + environment + "/config.properties";

			InputStream is = ConfigsLoader.class.getResourceAsStream(configPath);
			try {
				properties.load(is);
			} catch (IOException e) {

				LOG.warn("Property file not found");
				LOG.error(e.getMessage(), e);
			}

			// Read properties
			graphdb = properties.getProperty("graphdb");
			bugPredictorServer = properties.getProperty("bugPredictorServer");
			ansiblePath = properties.getProperty("ansiblePath");
			reasonerServer = properties.getProperty("reasonerServer");
			
			//omit for development purposes
			keycloak = properties.getProperty("keycloak");
			keycloakClientId = properties.getProperty("keycloakClientId");
			keycloakClientSecret = properties.getProperty("keycloakClientSecret");
		} else if (environment.equals("docker")) {
			graphdb = System.getenv("graphdb");
			bugPredictorServer = System.getenv("bugPredictorServer");
			ansiblePath = System.getenv("ansiblePath");
			reasonerServer = System.getenv("reasonerServer");
			
			kbUsername = System.getenv("kbUsername");
			kbPassword = System.getenv("kbPassword");
			
			keycloak = System.getenv("keycloak");
			keycloakClientId = System.getenv("keycloakClientId");
			keycloakClientSecret = System.getenv("keycloakClientSecret");			
		}
		LOG.info("graphdb = {}, bugpredictorServer = {}, ansiblePath = {}, reasonerServer = {}, keycloak = {}, keycloakClientId = {}", graphdb, bugPredictorServer, ansiblePath, reasonerServer, keycloak, keycloakClientId);
	}
	
	public String getEnvironment() {
		return environment;
	}
	
	public String getGraphdb() {
		return graphdb;
	}
	
	public String getBugPredictorServer() {
		return bugPredictorServer;
	}
	
	public String getAnsiblePath() {
		return ansiblePath;
	}
	public String getKbUsername() {
		return kbUsername;
	}
	
	public String getKbPassword() {
		return kbPassword;
	}
	
	public String getReasonerServer() {
		return reasonerServer;
	}
	
	public String getKeycloak() {
		return keycloak;
	}
	
	public String getKeycloakClientId() {
		return keycloakClientId;
	}
	
	public String getKeycloakClientSecret() {
		return keycloakClientSecret;
	}
	

	
	public static void main(String[] args) throws IOException {
		ConfigsLoader configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
}

