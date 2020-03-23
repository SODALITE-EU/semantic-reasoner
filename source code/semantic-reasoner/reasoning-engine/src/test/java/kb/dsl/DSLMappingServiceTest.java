package kb.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import kb.dsl.exceptions.MappingException;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.validation.exceptions.ValidationException;
import kb.validation.exceptions.models.ValidationModel;

class DSLMappingServiceTest {

	private static KB kb;
	private String aadmTTL;
	
	@BeforeAll
	static void beforeAll() {
		String getenv = System.getenv("graphdb");
		if (getenv != null)
			kb = new KB(getenv, "TOSCA");
		else
			kb = new KB();
	}

	@Test
	void testRequiredProperties() {
		try {
			aadmTTL = MyUtils.fileToString("dsl/ide_snow_v3.ttl");
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
				assertEquals(7,validationModels.size());
				System.out.println("Test Passed: Seven required properties are missing");
			} finally {
				m.shutDown();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
