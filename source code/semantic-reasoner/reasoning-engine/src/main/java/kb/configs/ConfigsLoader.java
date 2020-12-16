package kb.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class ConfigsLoader {

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
	public static final List AUTHENVS = Collections.unmodifiableList(
		    Arrays.asList(new String[] {"qa", "docker"}));
	
	
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
		System.out.println("environment = " + environment);

		if (environment == null) {
			environment = "dev";

			String configPath = "/envs/" + environment + "/config.properties";

			InputStream is = ConfigsLoader.class.getResourceAsStream(configPath);
			try {
				properties.load(is);
			} catch (IOException e) {

				System.out.println("Property file not found");
				e.printStackTrace();
			}

			// Read properties
			graphdb = properties.getProperty("graphdb");
			bugPredictorServer = properties.getProperty("bugPredictorServer");
			ansiblePath = properties.getProperty("ansiblePath");
			reasonerServer = properties.getProperty("reasonerServer");
			
			//omit for developement purposes
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
		System.out.println(String.format("graphdb = %s, bugpredictorServer = %s, ansiblePath = %s, reasonerServer = %s, keycloak = %s, keycloakClientId = %s", graphdb, bugPredictorServer, ansiblePath, reasonerServer, keycloak, keycloakClientId));
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
