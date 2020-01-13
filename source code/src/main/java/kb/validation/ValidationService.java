package kb.validation;

import java.io.IOException;

import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;

public class ValidationService {

	RequirementValidation requirementValidation;

	public ValidationService() {
		requirementValidation = new RequirementValidation();
	}

	public void validate() {
		try {
			requirementValidation.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoRequirementDefinitionValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NodeMismatchValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CapabilityMismatchValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (requirementValidation != null)
				requirementValidation.shutDown();
		}
	}

}
