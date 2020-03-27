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

import org.junit.jupiter.api.Test;

import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.repository.SodaliteRepository;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

class DSLMappingServiceTest {
	
	private static SodaliteRepository repositoryManager;
	private static Repository repository;
	private static KB kb;

	static void loadRepository() {
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
			InputStream input =
				DSLMappingServiceTest.class.getResourceAsStream("/snow/snow_tier1.ttl");
			repositoryConnection.add(input, "", RDFFormat.TURTLE);
		} catch (IOException e) {
			e.printStackTrace();
		}

		repositoryConnection.close();
	}

	static void removeRepository() {
		repository.shutDown();
		repositoryManager.removeRepository("SEMANTIC_REASONER_TEST");
		repositoryManager.shutDown("TEST");
	}
	
	//Test for a snow aadm is mapped without any error
	@Test
	void testDSLMappingService() {
		
		loadRepository();
		IRI aadmIRI = null;
		try {
			String aadmTTL = fileToString("dsl/ide_snow_v3.ttl");
			DSLMappingService m  = new DSLMappingService(kb, aadmTTL, "test");
			try {
				aadmIRI = m.start();
				m.save();
			} catch (MappingException e) {
				e.printStackTrace();
			} catch (ValidationException e) {
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m.shutDown();
				removeRepository();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertNotNull(aadmIRI);
		System.out.println("Test Passed: aadm for snow");
	}
	
	/* Test for required properties that are missing
	   The required registry_ip property has been removed from Template_2. */
	@Test
	void testMissingRequiredProperty() {
		loadRepository();
		try {
			String aadmTTL = fileToString("dsl/ide_snow_v3_required_property_missing.ttl");
			DSLMappingService m  = new DSLMappingService(kb, aadmTTL, "test");
			try {
				m.start();
				m.save();
			} catch (MappingException e) {
				e.printStackTrace();
			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
				assertEquals(validationModels.size(),1);
				System.out.println("Test Passed: registry_ip required property is missing");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m.shutDown();
				removeRepository();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	/* Test for a Mapping Exception.
	   The  name of Template_3 has been removed. */
	@Test
	void testMappingException() {
		loadRepository();
		try {
			String aadmTTL = fileToString("dsl/ide_snow_v3_mapping_exception.ttl");
			DSLMappingService m  = new DSLMappingService(kb, aadmTTL, "test");
			try {
				m.start();
				m.save();
			} catch (MappingException e) {
				System.out.println("Test Passed for MappingException: " + e.getMessage());
				assertEquals(e.getMessage(),"No 'name' defined for template: Template_3");
			} catch (ValidationException e) {	
				List<ValidationModel> validationModels = e.validationModels;
				for (ValidationModel validationModel : validationModels) {
					System.out.println("validationModel" + validationModel.toJson());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				m.shutDown();
				removeRepository();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
	
	public static String fileToString(String file) throws IOException {
		InputStream resourceAsStream = DSLMappingServiceTest.class.getClassLoader().getResourceAsStream(file);
		return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
	}
	
}
