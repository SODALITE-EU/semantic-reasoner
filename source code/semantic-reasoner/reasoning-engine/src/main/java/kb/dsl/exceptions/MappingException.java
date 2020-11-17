package kb.dsl.exceptions;

import java.util.List;

import kb.dsl.exceptions.models.DslValidationModel;

public class MappingException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public List<DslValidationModel> mappingValidationModels;

	public MappingException(List<DslValidationModel> mappingValidationModels) {
		this.mappingValidationModels = mappingValidationModels;
	}
	public MappingException(String a) {
		super(a);
	}


}
