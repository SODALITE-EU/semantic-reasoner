package kb.dto;

import java.io.IOException;
import java.util.HashSet;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;

import kb.utils.MyUtils;

public abstract class Resource {

	String uri;
	String label;
	String description;

	HashSet<String> relevantUris;

	public Resource(IRI iri) {
		uri = iri.toString();
		label = iri.getLocalName();

		relevantUris = new HashSet<String>();
	}

	@Override
	public String toString() {
		return MyUtils.getGson(true).toJson(this);
	}

	abstract public JsonElement serialise() throws IOException;

	abstract public JsonElement serialiseCompact();

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}
	
	public String getLabel() {
		return label;
	}

	public void setlabel(String label) {
		this.label = label;
	}

	public String getName() {
		return label;
	}

	public void setName(String name) {
		this.label = name;
	}
	
	public JsonElement serialiseForInterfaces() {
		return null;
	}

}
