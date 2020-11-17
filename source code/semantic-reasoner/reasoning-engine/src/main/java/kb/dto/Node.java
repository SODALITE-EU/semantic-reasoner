package kb.dto;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.utils.MyUtils;

public class Node extends Resource {

	IRI type;
	IRI namespace;
	IRI namespaceOfType;
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
		node.add(uri, data);

		return node;
	}

	public static void main(String[] args) {
		JsonObject node = new JsonObject();
		node.add("node", null);
		System.err.println(node);
	}

	@Override
	public JsonElement serialiseCompact() {
		// TODO Auto-generated method stub
		return null;
	}

}
