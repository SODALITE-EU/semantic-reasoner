package kb.dsl;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.KBApi;
import kb.dsl.exceptions.MappingException;
import kb.dsl.util.RepositoryTestUtils;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeleteModelTest {
	private static final Logger LOG = LoggerFactory.getLogger(DeleteModelTest.class.getName());
	private static final String SEMANTIC_REASONER_TEST = "SEMANTIC_REASONER_TEST";
		
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	private static KBApi api;

	static DSLRMMappingService rm1;
	static IRI rmIRI1;

	@BeforeAll
	 static void loadResourceModels() {
		LOG.info("loadResourceModels");
		repositoryManager = new SodaliteRepository(".", "/config.ttl");
		kb = new KB(repositoryManager, SEMANTIC_REASONER_TEST);
		api = new KBApi(kb);
		repository = repositoryManager.getRepository(SEMANTIC_REASONER_TEST);
			
		RepositoryConnection repositoryConnection = repository.getConnection();
		RepositoryTestUtils.loadCoreOntologies(repositoryConnection);		
		
			
		LOG.info("Loading resource models");			
		try {
			String rmTTL1 = RepositoryTestUtils.fileToString("resource_models/modules.docker_registry.rm.ttl");
				
			rm1 = new DSLRMMappingService(kb, rmTTL1,  "https://www.sodalite.eu/ontologies/workspace/1/vbeit9auui3d3j0tdekbljfndl/RM_92aj0uo7t6l6u8mv5tmh99pjnb" , "docker","DSL","modules.docker_registry.rm.ttl");
			rmIRI1 = rm1.start();
			rm1.save();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} catch (MappingException e) {
			LOG.error(e.getMessage(), e);
		} catch (ValidationException e) {
			LOG.error(e.getMessage(), e);
		}
	}
	
	@Test
	void testDeleteModel() throws IOException {
		LOG.info("testDeleteModel");
		try {
			boolean success = api.deleteModel(rmIRI1.toString());
			assertTrue(success);
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Delete Model For URI");
	}
	
	@AfterAll
	public static void cleanUp() {
		api.shutDown();
		removeRepository();
	}
	
	static void removeRepository() {
		repository.shutDown();
		repositoryManager.removeRepository(SEMANTIC_REASONER_TEST);
		repositoryManager.shutDown("TEST");
	}

}
