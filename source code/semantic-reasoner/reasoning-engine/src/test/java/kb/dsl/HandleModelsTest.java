package kb.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.KBApi;
import kb.dsl.exceptions.MappingException;
import kb.dsl.util.RepositoryTestUtils;
import kb.dto.SodaliteAbstractModel;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HandleModelsTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(HandleModelsTest.class.getName());
	private static final String SEMANTIC_REASONER_TEST = "SEMANTIC_REASONER_TEST";
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	private static KBApi api;

	static DSLRMMappingService rm1;
	static IRI rmIRI1 = null;

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
			
			rm1  = new DSLRMMappingService(kb, rmTTL1,"", "docker","DSL","modules.docker_registry.rm.ttl");
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
	void testGetModels() throws IOException {
		LOG.info("testGetModels");
		try {
			Set<SodaliteAbstractModel> models = api.getModels("RM", KB.BASE_NAMESPACE + "docker/");
		
			SodaliteAbstractModel model = models.iterator().next();
			assertEquals(model.getUri(), rmIRI1.toString());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Get Models");
	}
	
	@Test
	void testGetModelForResource() throws IOException {
		LOG.info("testGetModel");
		try {
			SodaliteAbstractModel model = api.getModelForResource("sodalite.nodes.DockerRegistry", KB.BASE_NAMESPACE + "docker/");
			assertEquals(model.getUri(), rmIRI1.toString());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Get Model For Resource");
	}
	
	@Test
	void testGetModelForURI() throws IOException {
		LOG.info("testGetModelForURI");
		try {
			SodaliteAbstractModel model = api.getModelFromURI(rmIRI1.toString());
			assertEquals(model.getUri(), rmIRI1.toString());
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("Exception was thrown");
		}
		
		LOG.info("Test Passed: Get Model For URI");
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
