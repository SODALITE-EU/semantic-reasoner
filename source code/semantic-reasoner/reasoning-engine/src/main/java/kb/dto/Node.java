package kb.dto;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.utils.MyUtils;

public class Node extends Resource {

	IRI type;
	IRI namespace;
	IRI namespaceOfType;
	//The versioning is per AADM, so the version of the model is associated to the entities
	String version;
//	List<String> superTypes;

	public Node(IRI iri) {
		super(iri);
	}

	public IRI getType() {
		return type;
	}

	public void setType(IRI type) {
		this.type = type;
	}

	public IRI getNamespace() {
		return namespace;
	}
	
	public void setNamespace(IRI namespace) {
		this.namespace = namespace;
	}
	
	public IRI getNamespaceOfType() {
		return namespaceOfType;
	}
	
	public void setNamespaceOfType(IRI namespaceOfType) {
		this.namespaceOfType = namespaceOfType;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	@Override
	public JsonElement serialise() {

		JsonObject node = new JsonObject();

		JsonObject data = new JsonObject();
		data.addProperty("label", label);
		if (description != null)
			data.addProperty("description", description);
		
		if (namespaceOfType != null)
			data.add("type", MyUtils.getLabelIRINamespace(this.type, this.namespaceOfType));
		else
			data.add("type", MyUtils.getLabelIRIPair(this.type));

		if (namespace != null)
			data.addProperty("namespace", namespace.toString());
		if (version != null)
			data.addProperty("version", version);
		node.add(uri, data);

		return node;
	}

	public static void main(String[] args) {
		JsonObject node = new JsonObject();
		node.add("node", null);
	}

	@Override
	public JsonElement serialiseCompact() {
		// TODO Auto-generated method stub
		return null;
	}
	

}
