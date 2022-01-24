package kb.repository;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import kb.configs.ConfigsLoader;
import kb.utils.MyFileUtil;

import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KB {
	private static final Logger LOG = LoggerFactory.getLogger(KB.class.getName());
	
    public static final String SERVER_URL = "http://localhost:7200";
    public static final String ANSIBLE = MyFileUtil.REASONER_SERVER + "Ansibles/";
    public static final String REPOSITORY = "TOSCA";

    public static final String PREFIXES = "PREFIX tosca: <https://www.sodalite.eu/ontologies/tosca/> \r\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \r\n" +
            "PREFIX soda: <https://www.sodalite.eu/ontologies/sodalite-metamodel/> \r\n" +
            "PREFIX DUL: <http://www.loa-cnr.it/ontologies/DUL.owl#> \r\n" +
            "PREFIX dcterms: <http://purl.org/dc/terms/> \r\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" +
            "PREFIX exchange: <https://www.sodalite.eu/ontologies/exchange/> \r\n";

    public static final String OPT_PREFIXES = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \r\n" +
            "PREFIX opt: <https://www.sodalite.eu/ontologies/optimizations#> \r\n" +
            "PREFIX owl: <http://www.w3.org/2002/07/owl#> \r\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \r\n" +
            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \r\n";

    public static final String SODA_DUL_PREFIXES = "PREFIX soda: <https://www.sodalite.eu/ontologies/sodalite-metamodel/> \r\n" +
			"PREFIX DUL: <http://www.loa-cnr.it/ontologies/DUL.owl#> \r\n";
    
    public static final String OWL_PREFIX = "PREFIX owl: <http://www.w3.org/2002/07/owl#>\r\n";
    public static final String SODA_PREFIX = "PREFIX soda: <https://www.sodalite.eu/ontologies/sodalite-metamodel/>\r\n";		

    public static final String BASE_NAMESPACE = "https://www.sodalite.eu/ontologies/workspace/1/";
    public static final String DCTERMS = "http://purl.org/dc/terms/";
    public static final String DUL = "http://www.loa-cnr.it/ontologies/DUL.owl#";
    public static final String TOSCA = "https://www.sodalite.eu/ontologies/tosca/";
    public static final String SODA = "https://www.sodalite.eu/ontologies/sodalite-metamodel/";
    public static final String EXCHANGE = "https://www.sodalite.eu/ontologies/exchange/";
    public static final String GLOBAL = BASE_NAMESPACE +"global/";
    
    


    SodaliteRepository manager;
    public RepositoryConnection connection;
    public ValueFactory factory;

    public KB() {
        manager = new SodaliteRepository(SERVER_URL, "", "");
        connection = getRepositoryConnection(REPOSITORY);
        factory = connection.getValueFactory();
    }

    public KB(String repoName) {
        manager = new SodaliteRepository(SERVER_URL, "", "");
        connection = getRepositoryConnection(repoName);
        factory = connection.getValueFactory();
    }

    public KB(String serverUrl, String repoName) {
        ConfigsLoader configLoaderIns = ConfigsLoader.getInstance();
        String env = configLoaderIns.getEnvironment();
        System.setProperty("org.eclipse.rdf4j.repository.debug", "true");
        if (env == null) {
            configLoaderIns.loadProperties();
            env = configLoaderIns.getEnvironment();
        }
        if ("dev".equals(env)) {
            manager = new SodaliteRepository(serverUrl, "", "");
            connection = getRepositoryConnection(repoName);
            factory = connection.getValueFactory();
        } else {
            checkIfRepoExists(serverUrl, repoName);
        }
    }

    public KB(SodaliteRepository manager) {
        this.manager = manager;
        connection = getRepositoryConnection(REPOSITORY);
        factory = connection.getValueFactory();
    }

    public KB(SodaliteRepository manager, String repoName) {
        this.manager = manager;
        connection = getRepositoryConnection(repoName);
        factory = connection.getValueFactory();
    }

    public void update(Model model) {
        connection.begin();
        connection.add(model);
        connection.commit();
    }

    private RepositoryConnection getRepositoryConnection(String repoName) {
        Repository repository = manager.getRepository(repoName);
        if (repository == null) {
            throw new RepositoryException("There is no repository with name : " + repoName);
        }
        return repository.getConnection();
    }

    public void shutDown() {
        manager.shutDown(null);
    }

    private boolean checkIfRepoExists(String serverUrl, String repoName) {
    	manager = new SodaliteRepository(serverUrl, ConfigsLoader.getInstance().getKbUsername(), ConfigsLoader.getInstance().getKbPassword());
	
    	if (manager.getRepository(repoName) != null) {
            connection = getRepositoryConnection(repoName);
            factory = connection.getValueFactory();
            return true;
        }

        RepositoryManager repositoryManager = manager.getManager();
        repositoryManager.init();

        // Instantiate a repository graph model
        TreeModel graph = new TreeModel();

        // Read repository configuration file
        InputStream config = SodaliteRepository.class.getResourceAsStream("/repo_defaults.ttl");
        RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        rdfParser.setRDFHandler(new StatementCollector(graph));
        try {
            rdfParser.parse(config, RepositoryConfigSchema.NAMESPACE);
            config.close();
        } catch (RDFParseException | RDFHandlerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Retrieve the repository node as a resource
        Resource repositoryNode = Models.subject(graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElse(null);
        // Create a repository configuration object and add it to the repositoryManager
        RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);
        repositoryManager.addRepositoryConfig(repositoryConfig);

        connection = repositoryManager.getRepository(repoName).getConnection();
        factory = connection.getValueFactory();

        try {
            InputStream input;
            input =
                    KB.class.getResourceAsStream("/ontologies/import/DUL.rdf");
            connection.add(input, "", RDFFormat.RDFXML);

            input =
                    KB.class.getResourceAsStream("/ontologies/core/tosca-builtins.ttl");
            connection.add(input, "", RDFFormat.TURTLE);

            input =
                    KB.class.getResourceAsStream("/ontologies/core/sodalite-metamodel.ttl");
            connection.add(input, "", RDFFormat.TURTLE);

            input =
                    KB.class.getResourceAsStream("/ontologies/core/optimizations.ttl");
            connection.add(input, "", RDFFormat.TURTLE);
        } catch (IOException e) {
        	LOG.error(e.getMessage(), e);
        }

        return false;
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
