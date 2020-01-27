package kb.repository;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class KB {

	public static String SERVER_URL = "http://localhost:7200";
	public static String ANSIBLE = "http://160.40.52.200:8084/Ansibles/";
	public static String REPOSITORY = "TOSCA";

	public static String PREFIXES = "PREFIX tosca: <https://www.sodalite.eu/ontologies/tosca/> \r\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \r\n" +
			"PREFIX soda: <https://www.sodalite.eu/ontologies/sodalite-metamodel/> \r\n" +
			"PREFIX DUL: <http://www.loa-cnr.it/ontologies/DUL.owl#> \r\n" +
			"PREFIX dcterms: <http://purl.org/dc/terms/> \r\n" +
			"PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" +
			"PREFIX exchange: <https://www.sodalite.eu/ontologies/exchange/> \r\n";

	public static String DCTERMS = "http://purl.org/dc/terms/";
	public static String DUL = "http://www.loa-cnr.it/ontologies/DUL.owl#";
	public static String TOSCA = "https://www.sodalite.eu/ontologies/tosca/";
	public static String SODA = "https://www.sodalite.eu/ontologies/sodalite-metamodel/";
	public static String EXCHANGE = "https://www.sodalite.eu/ontologies/exchange/";

	SodaliteRepository manager;
	public RepositoryConnection connection;
	public ValueFactory factory;

	public KB() {
		manager = new SodaliteRepository(SERVER_URL, "", "");
		connection = manager.getRepository(REPOSITORY).getConnection();
		factory = connection.getValueFactory();
	}

	public KB(String repoName) {
		manager = new SodaliteRepository(SERVER_URL, "", "");
		connection = manager.getRepository(repoName).getConnection();
		factory = connection.getValueFactory();
	}

	public KB(String serverUrl, String repoName) {
		manager = new SodaliteRepository(serverUrl, "", "");
		connection = manager.getRepository(repoName).getConnection();
		factory = connection.getValueFactory();
	}

	public KB(SodaliteRepository manager) {
		this.manager = manager;
		connection = manager.getRepository(REPOSITORY).getConnection();
		factory = connection.getValueFactory();
	}

	public KB(SodaliteRepository manager, String repoName) {
		this.manager = manager;
		connection = manager.getRepository(repoName).getConnection();
		factory = connection.getValueFactory();
	}

	public void update(Model model) {
		connection.begin();
		connection.add(model);
		connection.commit();
	}

	public void shutDown() {
		manager.shutDown(null);
	}

	public static void main(String[] args) {
		KB api = new KB();
		api.shutDown();
	}

	public SodaliteRepository getManager() {
		return manager;
	}

	public RepositoryConnection getConnection() {
		return connection;
	}

	public ValueFactory getFactory() {
		return factory;
	}

}
