package kb.validation.constraints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;


import kb.dsl.utils.NamedResource;
import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.models.ConstraintPropertyModel;

public class ConstraintsPropertyValidation {
	private static final Logger LOG = LoggerFactory.getLogger(ConstraintsPropertyValidation.class.getName());
	private String templateName;
	private NamedResource templateType;
	private Map<String, String> exchangePropertyValues;
	private KB kb;
	
	private List<ConstraintPropertyModel> models = new ArrayList<ConstraintPropertyModel>();
	
	private List<ConstraintResult> results = new ArrayList<ConstraintResult>();
	
	private String constraintsQuery = "select distinct ?property ?constraint ?constr_type ?value\r\n" + 
			"where {\r\n" + 
			"   ?var soda:hasInferredContext ?context .\r\n" + 
			"	?context tosca:properties ?concept .\r\n" + 
			"	?concept DUL:classifies ?property .\r\n" + 
			"	?concept DUL:hasParameter [DUL:classifies tosca:constraints; DUL:hasParameter [DUL:classifies ?constraint; tosca:hasDataValue ?value]].\r\n" + 
			"   ?concept DUL:hasParameter [DUL:classifies tosca:type; tosca:hasValue ?constr_type].\r\n" + 
			"}";
	
	public ConstraintsPropertyValidation(String templateName, NamedResource templateType, Map<String, String> exchangePropertyValues, KB kb) {
		this.templateName = templateName;
		this.templateType = templateType;
		this.exchangePropertyValues = exchangePropertyValues;
		this.kb = kb;
	}

	public List<ConstraintPropertyModel> validate() {
		String query = KB.PREFIXES + constraintsQuery;
		String type = templateType.getResourceURI();
		if (type == null)
			type = KB.TOSCA + templateType.getResource();
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("var", kb.factory.createIRI(type)));

		LOG.info("exchange properties values: {}", exchangePropertyValues);
		
		while(result.hasNext()) {
			BindingSet bindingSet = result.next();
			String prop = MyUtils.getStringValue(bindingSet.getBinding("property").getValue());
			String constr = MyUtils.getStringValue(bindingSet.getBinding("constraint").getValue());
			String constr_type = MyUtils.getStringValue(bindingSet.getBinding("constr_type").getValue());
			String type_value = bindingSet.getBinding("value").getValue().stringValue();

			ConstraintResult c = new ConstraintResult(prop, constr, constr_type, type_value);
			results.add(c);
		}
		
		for (Map.Entry<String, String> e : exchangePropertyValues.entrySet()) {
			
			for (ConstraintResult c: results) {
				if (e.getKey().equals(c.property)) {
					String templateValue = e.getValue();
					ConstraintsEnum constraint = ConstraintsEnum.valueOf(c.constraint.toUpperCase());
					switch (constraint) {
						case GREATER_OR_EQUAL:
							if (!ConstraintsEnum.GREATER_OR_EQUAL.compare(templateValue, c.value, c.const_type)) {
								models.add(new ConstraintPropertyModel(templateName, templateType.getResource(), 
										c.property, "template value: " + templateValue + " not greater or equal than constraint definition: " + c.value));
							}
						break;
						case LESS_OR_EQUAL:
							if (!ConstraintsEnum.LESS_OR_EQUAL.compare(templateValue, c.value, c.const_type)) {
								models.add(new ConstraintPropertyModel(templateName, templateType.getResource(), 
										c.property, "template value: " + templateValue + " not less or equal than constraint definition: " + c.value));	
							}
						break;
						default:
							break;
					}
				}
			}
		}
		
		return models;
	}

	private class ConstraintResult {
		String property;
		String constraint;
		String const_type;
		String value;
		
		ConstraintResult(String property, String constraint, String const_type, String value) {
			this.property = property;
			this.constraint = constraint;
			this.const_type = const_type;
			this.value = value;
		}
	}
}
