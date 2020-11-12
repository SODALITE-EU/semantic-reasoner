package kb.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import org.eclipse.rdf4j.rio.RDFFormat;

import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import kb.dsl.exceptions.MappingException;
import kb.dsl.exceptions.models.DslValidationModel;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DSLMappingServiceTest {
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;
	static DSLMappingService m;
	static DSLRMMappingService rm1;
	static DSLRMMappingService rm2;
	static DSLRMMappingService rm3;
	static DSLRMMappingService rm4; 

	@BeforeAll
	 static void loadRepository() {
		System.out.println("loadRepository");
		repositoryManager = new SodaliteRepository(".", "/config.ttl");
		kb = new KB(repositoryManager, "SEMANTIC_REASONER_TEST");

		repository = repositoryManager.getRepository("SEMANTIC_REASONER_TEST");
		
		RepositoryConnection repositoryConnection = repository.getConnection();
		try {
			InputStream input =
				DSLMappingServiceTest.class.getResourceAsStream("/import/DUL.rdf");
			repositoryConnection.add(input, "", RDFFormat.RDFXML);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			InputStream input =
					DSLMappingServiceTest.class.getResourceAsStream("/core/sodalite-metamodel.ttl");
			repositoryConnection.add(input, "", RDFFormat.TURTLE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			InputStream input =
				DSLMappingServiceTest.class.getResourceAsStream("/core/tosca-builtins.ttl");
			repositoryConnection.add(input, "", RDFFormat.TURTLE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			System.out.println("Loading resource models");
			//InputStream input =
				//DSLMappingServiceTest.class.getResourceAsStream("/snow/snow_tier1.ttl");
				//repositoryConnection.add(input, "", RDFFormat.TURTLE, kb.getFactory().createIRI("https://www.sodalite.eu/ontologies/workspace/1/snow/"));
			//IRI rmIRI1, rmIRI2, rmIRI3, rmIRI4;
			//String rmTTL1 = fileToString("snow/modules.docker_registry.rm.ttl");
			//String rmTTL2 = fileToString("snow/modules.docker_component.rm.ttl");
			String rmTTL3 = fileToString("snow/modules.openstack_security_rule.rm.ttl");
			//String rmTTL4 = fileToString("snow/modules.openstack_vm.rm.ttl");
			
			
			//rm1  = new DSLRMMappingService(kb, rmTTL1,"", "docker","DSL","");
			//rm2  = new DSLRMMappingService(kb, rmTTL2,"", "docker","DSL","");
			rm3  = new DSLRMMappingService(kb, rmTTL3,"", "openstack","DSL","");
			//rm4  = new DSLRMMappingService(kb, rmTTL4,"", "openstack","DSL","");
			
			try {
				/*rm1.start();
				rm1.save();
				
				rm2.start();
				rm2.save();*/
				
				rm3.start();
				rm3.save();
				
				/*rm4.start();
				rm4.save();*/
			} catch (MappingException e) {
				e.printStackTrace();
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

		repositoryConnection.close();
	}

	
	@AfterAll
	public static void cleanUp() {
		//rm1.shutDown();
		//rm2.shutDown();
		rm3.shutDown();
		//rm4.shutDown();
		removeRepository();
	}
	
	static void removeRepository() {
		repository.shutDown();
		repositoryManager.removeRepository("SEMANTIC_REASONER_TEST");
		repositoryManager.shutDown("TEST");
	}
	
	//Test for a snow aadm. It is mapped without any error
	@Test
	void testDSLMappingService() {

		IRI aadmIRI = null;
		DSLMappingService m = null;
		try {
			String aadmTTL = fileToString("dsl/ide_snow_v3.ttl");
			m  = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
			try {
				aadmIRI = m.start();
				m.save();
			} catch (MappingException e) {
				e.printStackTrace();
				m.shutDown();
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
				m.shutDown();
				return;
			} catch (Exception e) {
				e.printStackTrace();
				m.shutDown();
			}
		} catch (IOException e) {
			e.printStackTrace();
			m.shutDown();
		}
		assertNotNull(aadmIRI);
		System.out.println("Test Passed: aadm for snow");
	}
	
	/* Test for required properties that are missing
	   The required registry_ip property has been removed from Template_2. */
	@Test
	void testMissingRequiredProperty() {
		System.out.println("testMissingRequiredProperty");
		DSLMappingService m = null;
		try {
			String aadmTTL = fileToString("dsl/ide_snow_v3_required_property_missing.ttl");
			m = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
			try {
				m.start();


			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
				assertEquals(validationModels.size(),1);
				System.out.println("Test Passed: group_description required property is missing");
				return;
			} catch (Exception e) {
				m.shutDown();
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		m.shutDown();
		assertTrue(false);
	}
	
	/* Test for a Mapping Exception.
	   The  name of Template_3 has been removed. */
	@Test
	void testMappingException() {

		try {
			String aadmTTL = fileToString("dsl/ide_snow_v3_mapping_exception.ttl");
			DSLMappingService m = new DSLMappingService(kb, aadmTTL,"", false,"snow","DSL","snow.ttl");
			try {
				m.start();
				m.save();
			} catch (MappingException e) {
				System.out.println("Test Passed for MappingException: " + e.getMessage());
				List<DslValidationModel> validationModels = e.mappingValidationModels;
				for (DslValidationModel v : validationModels) {
					System.out.println("validationModel" + v.toJson());
				}
				assertEquals(validationModels.size(),1);
				return;
			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m.shutDown();

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		m.shutDown();
		assertTrue(false);
	}
	
	public static String fileToString(String file) throws IOException {
		InputStream resourceAsStream = DSLMappingServiceTest.class.getClassLoader().getResourceAsStream(file);
		return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
	}
	
}
