package kb.validation.exceptions.models;

import org.json.simple.JSONObject;

public class RequirementModel extends ValidationModel {
	String templateName, requirement, type_r_a, r_d, nodeType, validationType;

	public RequirementModel(String templateName, String requirement, String validationType) {
		super();
		this.templateName = templateName;
		this.requirement = requirement;
		this.validationType = validationType;
	}
	
	public RequirementModel(String templateName, String requirement, String type_r_a, String r_d, String validationType) {
		super();
		this.templateName = templateName;
		this.requirement = requirement;
		this.type_r_a = type_r_a;
		this.r_d = r_d;
		this.validationType = validationType;
	}
	
	//related to capabilities and relationships
	public RequirementModel(String templateName, String requirement, String type_r_a, String r_d, String nodeType, String validationType) {
		super();
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
		info.put("context", templateName);
		switch (validationType) {
			case "NoRequirementDefinition":
				info.put("description", String.format("There is no requirement definition for %s of %s", requirement, templateName));
				break;
			case "NodeMismatch":
				info.put("description",String.format("The type of requirement assigment %s of template %s does not match the requirement definition %s.",
						type_r_a, templateName, r_d));
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
