package kb.dsl.exceptions.models;

import org.json.simple.JSONObject;

import kb.validation.exceptions.models.ValidationModel;

public class MappingValidationModel extends DslValidationModel {

	String resourceName, element, typeOfElement, description;
	
	public MappingValidationModel(String resourceName, String element, String description) {
		this.resourceName = resourceName;
		this.element = element;
		this.description = description;
	}
	
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("type", "MappingException");

		JSONObject info = new JSONObject();
		info.put("description", description);
		info.put("name", element);
		info.put("context", resourceName);

		json.put("info", info);

		return json;

	}
	public String toString() {
		return "resourceName = " + resourceName + " ,element = " + element + ", typeOfElement =" + typeOfElement + "\ndescription=" + description;
	}
}
