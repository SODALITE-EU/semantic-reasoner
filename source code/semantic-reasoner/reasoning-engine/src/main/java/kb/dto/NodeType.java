package kb.dto;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class NodeType extends Resource {

	public NodeType(IRI iri) {
		super(iri);
	}

	@Override
	public JsonElement serialise() {

		JsonObject nodeType = new JsonObject();

		JsonObject data = new JsonObject();
		data.addProperty("label", label);
		if (description != null)
			data.addProperty("description", description);
		
		nodeType.add(uri, data);
		return nodeType;
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
