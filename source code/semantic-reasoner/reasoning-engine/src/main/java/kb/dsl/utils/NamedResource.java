package kb.dsl.utils;

public class NamedResource {
	String namespace;
	String resource; 
	String resourceURI;

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public String getResource() {
		return this.resource;
	}

	public void setResource(String resource) {
		this.resource = resource;
	}
	
	public String getResourceURI() {
		return this.resourceURI;
	}

	public void setResourceURI(String resourceURI) {
		this.resourceURI = resourceURI;
	}
	
}
