package kb.dto;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.utils.MyUtils;

public class Node extends Resource {

	IRI type;
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

	@Override
	public JsonElement serialise() {

		JsonObject node = new JsonObject();

		JsonObject data = new JsonObject();
		data.addProperty("label", label);
		if (description != null)
			data.addProperty("description", description);
		data.add("type", MyUtils.getLabelIRIPair(this.type));
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
