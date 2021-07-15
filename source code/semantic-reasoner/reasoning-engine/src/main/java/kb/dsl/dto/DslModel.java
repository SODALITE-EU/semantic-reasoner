package kb.dsl.dto;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class DslModel {
	String uri;
	String version;
	IRI fullUri;
	
	
	public DslModel(String uri, IRI fullUri, String version) {
		this.uri = uri;
		this.version = version;
		this.fullUri = fullUri;
	}
	
	public String getUri() {
		return this.uri;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public IRI getFullUri() {
		return this.fullUri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public void setFullUri(IRI fullUri) {
		this.fullUri = fullUri;
	}

}