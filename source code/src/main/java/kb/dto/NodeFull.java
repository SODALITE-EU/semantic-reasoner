package kb.dto;

import java.io.IOException;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;

public class NodeFull extends Node {

	Set<Property> properties;
	Set<Attribute> attributes;
	Set<Capability> capabilities;
	Set<Requirement> requirements;
	Set<Interface> interfaces;
	Set<IRI> validTargetTypes;
	Set<Operation> operations;

	boolean isTemplate = false;

	public NodeFull(IRI iri, boolean isTemplate) {
		super(iri);
		this.isTemplate = isTemplate;
	}

	public void build(KBApi api) throws IOException {
		System.out.println("building: " + uri + ", isTemplate: " + isTemplate);

		properties = api.getProperties(uri.toString(), isTemplate);
		attributes = api.getAttributes(uri.toString(), isTemplate);
		capabilities = api.getCapabilities(uri.toString(), isTemplate);
		interfaces = api.getInterfaces(uri.toString(), isTemplate);
		requirements = api.getRequirements(uri.toString(), isTemplate);

		// TODO orphan properties
		validTargetTypes = api.getValidTargetTypes(uri.toString(), isTemplate);
//		System.err.println("validTargetTypes " + validTargetTypes);

		operations = api.getOperations(uri.toString(), isTemplate);
//		System.err.println("validTargetTypes " + validTargetTypes);
	}

	public boolean isTemplate() {
		return isTemplate;
	}

//	public void setTemplate(boolean isTemplate) {
//		this.isTemplate = isTemplate;
//	}

	@Override
	public JsonElement serialise() {
		JsonObject data = new JsonObject();
		if (description != null)
			data.addProperty("description", description);
		data.addProperty("type", this.type.toString());
		relevantUris.add(this.type.toString());
		data.addProperty("isNodeTemplate", isTemplate);

		// properties
		JsonArray array = new JsonArray();
		for (Property p : properties) {
			array.add(p.serialise());
			relevantUris.addAll(p.relevantUris);
		}

		if (!properties.isEmpty())
			data.add("properties", array);

		// attributes
		array = new JsonArray();
		for (Attribute a : attributes) {
			array.add(a.serialise());
			relevantUris.addAll(a.relevantUris);
		}
		if (!attributes.isEmpty())
			data.add("attributes", array);

		// requirements
		array = new JsonArray();
		for (Requirement r : requirements) {
			array.add(r.serialise());
			relevantUris.addAll(r.relevantUris);
		}
		if (!requirements.isEmpty()) {
			data.add("requirements", array);
		}

		// capabilities
		array = new JsonArray();
		for (Capability c : capabilities) {
			array.add(c.serialise());
			relevantUris.addAll(c.relevantUris);
		}
		if (!capabilities.isEmpty())
			data.add("capabilities", array);

		// interfaces
		array = new JsonArray();
		for (Interface i : interfaces) {
			array.add(i.serialise());
			relevantUris.addAll(i.relevantUris);
		}
		if (!interfaces.isEmpty())
			data.add("interfaces", array);

		// valid_target_types
		array = new JsonArray();
		for (IRI i : validTargetTypes) {
			array.add(i.toString());
			relevantUris.add(i.toString());
		}
		if (!validTargetTypes.isEmpty())
			data.add("valid_target_types", array);

		// operations
		array = new JsonArray();
		for (Operation op : operations) {
			array.add(op.serialise());
			System.err.println("operations " + array);
		}

		if (!operations.isEmpty())
			data.add("operations", array);

		return data;
	}

}
