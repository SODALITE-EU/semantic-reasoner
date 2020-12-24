package kb.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.Model;

import kb.utils.MyFileUtil;
import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;
import kb.validation.exceptions.models.RequirementModel;

public class ValidationService {
	private static final Logger LOG = Logger.getLogger(ValidationService.class.getName());
	RequirementValidation requirementValidation;

	List<RequirementModel> models = new ArrayList<RequirementModel>();
	
	public ValidationService(Model model) {
		requirementValidation = new RequirementValidation(model);
	}
	
	public ValidationService(String aadmId) {
		requirementValidation = new RequirementValidation(aadmId);
	}

	public List<RequirementModel> validate() {
		try {
			models = requirementValidation.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			LOG.log(Level.SEVERE, e.getMessage(), e);
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
			requirementValidation.shutDown();
		}
		return models;
	}

}
