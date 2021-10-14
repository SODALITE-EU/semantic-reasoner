package kb.dsl.test;

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
import kb.dsl.DSLRMMappingService;
import kb.dsl.dto.DslModel;
import kb.dsl.exceptions.MappingException;
import kb.dsl.test.util.RepositoryTestUtils;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DeleteModelTest {
	private static final Logger LOG = LoggerFactory.getLogger(DeleteModelTest.class.getName());
		
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	private static KBApi api;

	static DSLRMMappingService rm1;
	static DslModel rmIRI1;

	@BeforeAll
	 static void loadResourceModels() {
		LOG.info("loadResourceModels");
		repositoryManager = new SodaliteRepository("target/", "/config.ttl");
		kb = new KB(repositoryManager, RepositoryTestUtils.SEMANTIC_REASONER_TEST);
		api = new KBApi(kb);
		repository = repositoryManager.getRepository(RepositoryTestUtils.SEMANTIC_REASONER_TEST);
			
		RepositoryConnection repositoryConnection = repository.getConnection();
		RepositoryTestUtils.loadCoreOntologies(repositoryConnection);		
		
			
		LOG.info("Loading resource models");			
		try {
			String rmTTL1 = RepositoryTestUtils.fileToString("resource_models/modules.docker_registry.rm.ttl");
				
			rm1 = new DSLRMMappingService(kb, rmTTL1,  "https://www.sodalite.eu/ontologies/workspace/1/vbeit9auui3d3j0tdekbljfndl/RM_92aj0uo7t6l6u8mv5tmh99pjnb" , "docker","DSL","modules.docker_registry.rm.ttl");
			rmIRI1 = rm1.start();
			
			//resave so as the delete of VerifySingularity to be checked
			rm1.save();
			rmIRI1 = rm1.start();
			rm1.save();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} catch (MappingException e) {
			LOG.error(e.getMessage(), e);
		} catch (ValidationException e) {
			LOG.error(e.getMessage(), e);
		}
		repositoryConnection.close();
	}
	
	@Test
	void testDeleteModel() throws IOException {
		LOG.info("testDeleteModel");
		try {
			LOG.info("rmIRI1: {}", rmIRI1.getFullUri().toString());
			boolean success = api.deleteModel(rmIRI1.getFullUri().toString(), "", false);
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
		RepositoryTestUtils.removeRepository(repository, repositoryManager);
	}
	
	
}
