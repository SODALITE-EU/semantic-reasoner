package kb.validation.exceptions;

import org.eclipse.rdf4j.model.IRI;

public class NodeMismatchValidationException extends Exception {

	private static final long serialVersionUID = 1L;

	public NodeMismatchValidationException(IRI type_r_a_node, IRI template, IRI r_d_node) {
		super(String.format(
				"The type of requirement assigment %s of template %s does not match the requirement definition %s.",
				type_r_a_node.getLocalName(), template.getLocalName(), r_d_node.getLocalName()));
	}

}
