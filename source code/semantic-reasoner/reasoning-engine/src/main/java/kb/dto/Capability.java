package kb.dto;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

public class Capability extends Feature {

	Set<Property> properties; 
	
	public Capability(IRI iri) {
		super(iri);
		// TODO Auto-generated constructor stub
	}

	public Set<Property> getProperties() {
		return this.properties;
	}
	
	public void setProperties(Set<Property> properties) {
		this.properties = properties;
	}
}
