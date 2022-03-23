package kb.validation.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.repository.KB;
import kb.repository.KBConsts;
import kb.validation.exceptions.models.PropertyInputModel;


public class PropertyInputValidation {
	private static final Logger LOG = LoggerFactory.getLogger(PropertyInputValidation.class.getName());

	IRI templateType;
	String templateName;
	HashMap<String, String> exhangeInputProperties;
	Set<String> exchangeInputNames;
	KB kb;
	Model aadm;
	String kindOfTemplate;
	
	List<PropertyInputModel> models = new ArrayList<PropertyInputModel>();

	public PropertyInputValidation(String kindOfTemplate, String templateName, 
								 HashMap<String, String> inputProperties, Set<String> inputNames, KB kb) {
		this.kindOfTemplate = kindOfTemplate;
		this.templateName = templateName;
		this.exhangeInputProperties = inputProperties;
		this.exchangeInputNames = inputNames;		
		this.kb = kb;
	}

	public List<PropertyInputModel> validate() {
		LOG.info("exchangeInputNames: {}", exchangeInputNames);
		LOG.info("exhangeInputProperties: {}", exhangeInputProperties);
		
		for (Map.Entry e : exhangeInputProperties.entrySet()) {
			 LOG.info("Property: {} with get_input: {}", e.getKey(), e.getValue());
			 String propertyName = (String)  e.getKey();
			 String contextPath = kindOfTemplate + KBConsts.SLASH +  templateName + KBConsts.SLASH + KBConsts.PROPERTIES + KBConsts.SLASH + propertyName + KBConsts.SLASH + KBConsts.GET_INPUT ;

			 String input = (String) e.getValue();
			 if (!exchangeInputNames.contains(input)) {
				 LOG.info("The {} is not present in the inputs of the model.", input);
				 String description = "The " + input + " is not present in the model";
				 models.add(new PropertyInputModel(contextPath, description, propertyName));
			 }
		}
		return models;
	}

}
