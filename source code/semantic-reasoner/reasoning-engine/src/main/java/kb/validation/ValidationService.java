package kb.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Model;

import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;
import kb.validation.exceptions.models.RequirementModel;

public class ValidationService {

	RequirementValidation requirementValidation;

	List<RequirementModel> models = new ArrayList<RequirementModel>();
	
	public ValidationService(Model model) {
		requirementValidation = new RequirementValidation(model);
	}

	public List<RequirementModel> validate() {
		try {
			models = requirementValidation.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}/* catch (NoRequirementDefinitionValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NodeMismatchValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CapabilityMismatchValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/ finally {
			if (requirementValidation != null)
				requirementValidation.shutDown();
		}
		return models;
	}

}
