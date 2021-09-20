package kb.validation.exceptions.models;

import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class RequirementModel extends ValidationModel {
	String contextPath, templateName, requirement, type_r_a, r_d, nodeType, validationType;
	Set<IRI> matchingNodes;

	public RequirementModel(String contextPath, String templateName, String requirement, String validationType) {
		super();
		this.contextPath = contextPath;
		this.templateName = templateName;
		this.requirement = requirement;
		this.validationType = validationType;
	}
	
	public RequirementModel(String contextPath, String templateName, String requirement, String type_r_a, String r_d, String validationType, Set<IRI> matchingNodes) {
		super();
		this.contextPath = contextPath;
		this.templateName = templateName;
		this.requirement = requirement;
		this.type_r_a = type_r_a;
		this.r_d = r_d;
		this.validationType = validationType;
		this.matchingNodes = matchingNodes;
	}
	
	//related to capabilities and relationships
	public RequirementModel(String contextPath, String templateName, String requirement, String type_r_a, String r_d, String nodeType, String validationType) {
		super();
		this.contextPath = contextPath;
		this.templateName = templateName;
		this.requirement = requirement;
		this.type_r_a = type_r_a;
		this.r_d = r_d;
		this.nodeType = nodeType;
		this.validationType = validationType;
	}

	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", validationType);

		JSONObject info = new JSONObject();
		info.put("context", contextPath);
		switch (validationType) {
			case "NoRequirementDefinition":
				info.put("description", String.format("There is no requirement definition for %s of %s", requirement, templateName));
				break;
			case "NodeMismatch":
				info.put("description",String.format("The type of requirement assigment %s of template %s does not match the requirement definition %s.",
						type_r_a, templateName, r_d));
				if (!matchingNodes.isEmpty()) {
					JSONArray nodes = new JSONArray();
					for (IRI t : matchingNodes) {
						nodes.add(t.toString());
					}
					
					info.put("suggestions", nodes);
				}
				break;
			case "CapabilityExistsMismatch":
			case "CapabilityMismatch":
				info.put("description",
				String.format("On requirement %s, node template %s has the capability type %s that does not match the capability of %s (%s).",
						requirement, templateName, type_r_a , nodeType , r_d));
				
				break;
			case "RelationshipMismatch":
				info.put("description",
						String.format("On requirement %s, node template %s has the relationship type %s that does not match the relationship of %s (%s).",
								requirement, templateName, type_r_a , nodeType , r_d));
				break;
		}
		info.put("name", requirement);
		json.put("info", info);

		return json;

	}
}
