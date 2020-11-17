package kb.utils;

import java.io.IOException;
import java.io.InputStream;
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
		} else if (environment.equals("docker")) {
			graphdb = System.getenv("graphdb");
			bugPredictorServer = System.getenv("bugPredictorServer");
			ansiblePath = System.getenv("ansiblePath");
			reasonerServer = System.getenv("reasonerServer");
			
			kbUsername = System.getenv("kbUsername");
			kbPassword = System.getenv("kbPassword");
		}
		System.out.println(String.format("graphdb = %s, bugpredictorServer = %s, ansiblePath = %s, reasonerServer = %s", graphdb, bugPredictorServer, ansiblePath, reasonerServer));
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
	
	public static void main(String[] args) throws IOException {
		ConfigsLoader configInstance = ConfigsLoader.getInstance();
		configInstance.loadProperties();
	}
}

