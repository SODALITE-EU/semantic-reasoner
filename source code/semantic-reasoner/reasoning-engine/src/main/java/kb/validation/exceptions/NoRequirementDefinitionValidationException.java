package kb.validation.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class NoRequirementDefinitionValidationException extends Exception {

	private static final long serialVersionUID = 1L;

	public NoRequirementDefinitionValidationException(IRI template, IRI r_a) {
		super(String.format("There is no requirement definition for %s of %s", r_a, template));
	}

}
