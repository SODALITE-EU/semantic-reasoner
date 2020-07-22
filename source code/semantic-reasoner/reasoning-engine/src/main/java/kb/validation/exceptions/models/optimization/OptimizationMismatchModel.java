package kb.validation.exceptions.models.optimization;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONObject;

import kb.validation.exceptions.models.ValidationModel;

import kb.utils.MyUtils;

public class OptimizationMismatchModel extends ValidationModel {

	IRI template;
	String path;
	String targetJson;
	String userOptValue;
	String expectedValue;
	
	public OptimizationMismatchModel(IRI template, String path, String targetJson, String userOptValue, String expectedValue) {
		this.template = template;
		this.path = path;
		this.targetJson = targetJson;
		this.userOptValue = userOptValue;
		this.expectedValue = expectedValue;
	}
	
	
	@Override
	public JSONObject toJson() {
		// TODO Auto-generated method stub
		String description  = String.format("\"%s\" (given value) != \"%s\" (expected value)", userOptValue, expectedValue);
				
		JSONObject json = new JSONObject();
		json.put("type", "OptimizationMismatch");

		JSONObject info = new JSONObject();
		info.put("description", description);
		info.put("context", template.toString());
		info.put("path", path);
		info.put("value", targetJson);

		json.put("info", info);
		return json;
	}
}
