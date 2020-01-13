package kb.validation.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class CapabilityMismatchValidationException extends Exception {

	private static final long serialVersionUID = 1L;

	public CapabilityMismatchValidationException(IRI template, IRI r_a, IRI nodeType, IRI templateCapabilityType,
			IRI r_d_capability) {
		super(String.format(
				"On requirement %s, node template %s has the capability type %s that does not match the capability of %s (%s).",
				r_a.getLocalName(), template.getLocalName(),
				templateCapabilityType.getLocalName(), nodeType.getLocalName(), r_d_capability.getLocalName()));
	}

}
