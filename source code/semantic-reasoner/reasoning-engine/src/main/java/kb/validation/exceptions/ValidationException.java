package kb.validation.exceptions;

import java.util.List;

import kb.validation.exceptions.models.ValidationModel;

public class ValidationException extends Exception {

	private static final long serialVersionUID = 1L;

	public List<ValidationModel> validationModels;

	public ValidationException(List<ValidationModel> validationModels) {
		this.validationModels = validationModels;
	}

}
