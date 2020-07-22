package kb.validation.exceptions.models.optimization;

import org.eclipse.rdf4j.model.IRI;
import org.json.simple.JSONObject;
import kb.validation.exceptions.models.ValidationModel;

import kb.utils.MyUtils;

public class ApplicationTypeModel  extends ValidationModel {

	String appType;
	IRI template;
	IRI templateType;
	
	public ApplicationTypeModel(String appType, IRI template) {
		this.appType = appType;
		this.template = template;
	}
	
	public ApplicationTypeModel(String appType, IRI template, IRI templateType) {
		this.appType = appType;
		this.template = template;
		this.templateType = templateType;
	}
	
	@Override
	public JSONObject toJson() {
		// TODO Auto-generated method stub
		JSONObject t = new JSONObject();
		String description = null;

		if (templateType != null)
			description = String.format("%s template of type %s is not an %s application", MyUtils.getStringValue(template), templateType, appType);
		else
			description = String.format("%s application type is not valid", appType);
				
		t.put("type", "Optimization");

		JSONObject info = new JSONObject();
		info.put("description", description);
		info.put("context", template.toString());

		t.put("info", info);
		return t;
	}
	
}
