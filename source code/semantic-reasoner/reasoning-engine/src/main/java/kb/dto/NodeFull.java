package kb.dto;

import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;
import kb.repository.KBConsts;

public class NodeFull extends Node {
	private static final Logger LOG = LoggerFactory.getLogger(NodeFull.class.getName());

	Set<Property> properties;
	Set<Attribute> attributes;
	Set<Capability> capabilities;
	Set<Requirement> requirements;
	Set<Interface> interfaces;
	Set<IRI> validTargetTypes;
	Set<Operation> operations;
	Set<Trigger> triggers;
	Set<IRI> targets;

	Set<Property> inputs;	
	Optimization optimization;

	//only for types
	String classType;


	public boolean isTemplate = false, isInput = false;

	public NodeFull(IRI iri, boolean isTemplate) {
		super(iri);
		this.isTemplate = isTemplate;
	}

	public void build(KBApi api) throws IOException {
		LOG.info("building={}, isTemplate={}", uri, isTemplate);

		properties = api.getProperties(uri.toString(), isTemplate, KBConsts.AADM_JSON);
		attributes = api.getAttributes(uri.toString(), isTemplate, KBConsts.AADM_JSON);
		capabilities = api.getCapabilities(uri.toString(), isTemplate, KBConsts.AADM_JSON);
		interfaces = api.getInterfaces(uri.toString(), isTemplate);
		requirements = api.getRequirements(uri.toString(), isTemplate);

		// TODO orphan properties
		validTargetTypes = api.getValidTargetTypes(uri.toString(), isTemplate);

		operations = api.getOperations(uri.toString(), isTemplate);
		
		optimization = api.getOptimization(uri.toString());
		//policies
		triggers = api.getTriggers(uri.toString(), isTemplate);
		targets = api.getTargets(uri.toString(), isTemplate);

		// inputs
		inputs = api.getInputs(uri.toString(), false);
		
		if (!isTemplate())
			classType = api.getClassForType(uri);
			
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
		if (!isInput) {
			if (description != null)
				data.addProperty("description", description);
			data.addProperty("type", this.type.toString());
			relevantUris.add(this.type.toString());
			data.addProperty("isNodeTemplate", isTemplate);
			if (!isTemplate() && classType != null)
				data.addProperty("class", classType);
		}
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
			LOG.info("operations {}", array);
		}

		if (!operations.isEmpty())
			data.add("operations", array);

		// inputs
		array = new JsonArray();
		for (Property p : inputs) {
			array.add(p.serialise());
			relevantUris.addAll(p.relevantUris);
		}

		if (!inputs.isEmpty())
			data.add("inputs", array);

		if(optimization != null)
			data.addProperty("optimization", optimization.getJson());
		
		// triggers
		array = new JsonArray();
		for (Trigger t : triggers) {
			array.add(t.serialise());
			relevantUris.addAll(t.relevantUris);
		}
		
		if (!triggers.isEmpty())
			data.add("triggers", array);
		
		array = new JsonArray();
		for (IRI t : targets) {
			array.add(t.toString());
			relevantUris.add(t.toString());
		}
		if (!targets.isEmpty())
			data.add("targets", array);
		
		return data;
	}

}
