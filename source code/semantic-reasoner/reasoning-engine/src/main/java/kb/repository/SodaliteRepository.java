package kb.repository;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SodaliteRepository {
	private static final Logger LOG = LoggerFactory.getLogger(SodaliteRepository.class.getName());
	private RepositoryManager _manager;

	public SodaliteRepository(String serverURL, String username, String password) {

		try {
			_manager = new RemoteRepositoryManager(serverURL);
			((RemoteRepositoryManager)_manager).setUsernameAndPassword(username, password);
			_manager.init();
		} catch (RepositoryException e) {
			LOG.error(e.getMessage(), e);
		}

	}

	public SodaliteRepository(String localLoc, String configLoc) {
		try {
			_manager = new LocalRepositoryManager(new File(localLoc));
			_manager.init();
			TreeModel graph = new TreeModel();
			InputStream config = SodaliteRepository.class.getResourceAsStream(configLoc);
			RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
			rdfParser.setRDFHandler(new StatementCollector(graph));
			try {
				rdfParser.parse(config, RepositoryConfigSchema.NAMESPACE);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
			try {
				config.close();
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}

			Resource repositoryNode = Models.subject(
					graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY)).orElse(null);

			RepositoryConfig repositoryConfig = RepositoryConfig.create(graph, repositoryNode);
			_manager.addRepositoryConfig(repositoryConfig);
		} catch (RepositoryException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public Repository getRepository(String id) throws RepositoryConfigException, RepositoryException {
		return _manager.getRepository(id);
	}

	public boolean removeRepository(String id) throws RepositoryConfigException, RepositoryException {
		return _manager.removeRepository(id);
	}

	public void shutDown(String CONTEXT) {
		LOG.info("closing GraphDb manager [ {}]\n", CONTEXT);
		if (_manager != null) {
			try {
				_manager.shutDown();
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
		}
	}

	public RepositoryManager getManager() {
		return _manager;
	}

}
