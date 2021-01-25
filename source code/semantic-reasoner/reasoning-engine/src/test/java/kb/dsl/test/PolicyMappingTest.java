package kb.dsl.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import java.util.Set;

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
import kb.dsl.DSLMappingService;
import kb.dsl.DSLRMMappingService;
import kb.dsl.exceptions.MappingException;
import kb.dsl.test.util.RepositoryTestUtils;
import kb.dto.Trigger;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PolicyMappingTest {
	private static final Logger LOG = LoggerFactory.getLogger(PolicyMappingTest.class.getName());
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	private static KBApi api;
	

	static DSLRMMappingService rm1;
	static DSLMappingService aadm1; 
	
	static RepositoryConnection repositoryConnection;

	@BeforeAll
	 static void loadResourceModels() {		
		LOG.info("loadRepository");
		
		repositoryManager = new SodaliteRepository("target/", "/config.ttl");
		kb = new KB(repositoryManager, RepositoryTestUtils.SEMANTIC_REASONER_TEST);
		api = new KBApi(kb);
		
		repository = repositoryManager.getRepository(RepositoryTestUtils.SEMANTIC_REASONER_TEST);
	
		repositoryConnection = repository.getConnection();
		
		RepositoryTestUtils.loadCoreOntologies(repositoryConnection);
	
	
	
		IRI  rmIRI1 = null;
		IRI aadmIRI = null;
		try {
			LOG.info("Loading resource models");			
			
			String rmTTL1 = RepositoryTestUtils.fileToString("resource_models/policies.test.rm.ttl");
			String aadmTTL1 = RepositoryTestUtils.fileToString("dsl/policy/policies.test.aadm.ttl");
			
			
			rm1  = new DSLRMMappingService(kb, rmTTL1,"", "radon","DSL","");
			aadm1  = new DSLMappingService(kb, aadmTTL1,"", false,  "radon","DSL","");
			
			try {
				rmIRI1 = rm1.start();
				rm1.save();
				assertNotNull(rmIRI1);
				
				aadmIRI = aadm1.start();
				aadm1.save();
				assertNotNull(aadmIRI);

				LOG.info("Test Passed: saving rm and aadm for policy");
			} catch (MappingException e) {
				LOG.error(e.getMessage(), e);
				fail("MappingException was thrown");
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					LOG.info("validationModel" + validationModel.toJson());
				}
				fail("ValidationException was thrown");
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
				fail("Exception was thrown");
			}
			
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
			fail("IOException was thrown");
		}
		
		repositoryConnection.close();
	}
	
	@Test
	void testTriggersService() throws IOException {
		LOG.info("getTriggers");
		
		Set<Trigger> triggers = api.getTriggers(api.getResourceIRI("radon/radon.policies.scaling.ScaleUp"), false);
		
		assertTrue(triggers.size() == 1);
		LOG.info("Test Passed: getTriggers of a policy type");
		
	}
	
	@AfterAll
	public static void cleanUp() {
		rm1.shutDown();
		RepositoryTestUtils.removeRepository(repository, repositoryManager);
	}
	
}
