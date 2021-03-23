package kb.validation.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.model.IRI;
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
	
	private List<ConstraintPropertyModel> models = new ArrayList<>();
	
	private List<ConstraintResult> results = new ArrayList<>();
	
	private String constraintsQuery = "select distinct ?property ?constraint ?constr_type ?value ?listvalue\r\n" + 
			"where {\r\n" + 
			"\t\t?var soda:hasInferredContext ?context .\r\n" + 
			"\t\t?context tosca:properties ?concept .\r\n" + 
			"\t\t?concept DUL:classifies ?property .\r\n" + 
			"\t\t{" +
			"\t\t?concept DUL:hasParameter [DUL:classifies tosca:constraints; DUL:hasParameter [DUL:classifies ?constraint; tosca:hasDataValue ?value]].\r\n" + 
			"\t\t} UNION {\r\n" +
			"\t\t?concept DUL:hasParameter [DUL:classifies tosca:constraints; DUL:hasParameter [DUL:classifies ?constraint; tosca:hasValue ?listvalue]].\r\n" +
			"\t\t?listvalue rdf:type tosca:List .\r\n" +
			"\t\t}\r\n" +
			"\t\t?concept DUL:hasParameter [DUL:classifies tosca:type; tosca:hasValue ?constr_type].\r\n" +
			"}\n";
	
	private String getListValuesQuery = "select  ?value\r\n" + 
			"where {\r\n" + 
			"\t\t?var rdf:type tosca:List .\r\n" + 
			"\t\t?var tosca:hasDataValue ?value\r\n" + 
			"\t}\r\n";
	
	public ConstraintsPropertyValidation(String templateName, NamedResource templateType, Map<String, String> exchangePropertyValues, KB kb) {
		this.templateName = templateName;
		this.templateType = templateType;
		this.exchangePropertyValues = exchangePropertyValues;
		this.kb = kb;
	}

	public List<ConstraintPropertyModel> validate() {
		String query = KB.PREFIXES + constraintsQuery;
		String listquery = KB.PREFIXES + getListValuesQuery;
		String type = templateType.getResourceURI();
		if (type == null)
			type = KB.TOSCA + templateType.getResource();
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("var", kb.factory.createIRI(type)));

		LOG.info("exchange properties values: {}", exchangePropertyValues);
		
		while(result.hasNext()) {
			BindingSet bindingSet = result.next();
			String prop = MyUtils.getStringValue(bindingSet.getBinding("property").getValue());
			String constr = MyUtils.getStringValue(bindingSet.getBinding("constraint").getValue());
			String constrType = bindingSet.getBinding("constr_type").getValue().stringValue();
			String typeValue = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue().stringValue() : null;
			IRI typeList = bindingSet.hasBinding("listvalue") ? (IRI) bindingSet.getBinding("listvalue").getValue() : null;

			ConstraintResult c;
			//get list query
			if (typeList != null) {
				TupleQueryResult result2 = QueryUtil.evaluateSelectQuery(kb.getConnection(), listquery, new SimpleBinding("var", typeList));
				Set<String> list = new HashSet<>();
				while(result2.hasNext()) {
					BindingSet bindingSet2 = result2.next();
					String value = MyUtils.getStringValue(bindingSet2.getBinding("value").getValue());
					list.add(value);
				}
				c = new ConstraintResult(prop, constr, constrType, list);
			} else {
				c = new ConstraintResult(prop, constr, constrType, typeValue);
			}
			LOG.info("constrType: {}, constraint: {}, property: {}", constrType, constr, prop);
			
			results.add(c);
		}
		
		for (Map.Entry<String, String> e : exchangePropertyValues.entrySet()) {
			
			for (ConstraintResult c: results) {
				if (e.getKey().equals(c.property)) {
					String templateValue = e.getValue();
					ConstraintsEnum constraint = ConstraintsEnum.valueOf(c.constraint.toUpperCase());
					switch (constraint) {
						case GREATER_OR_EQUAL:
							if (!ConstraintsEnum.GREATER_OR_EQUAL.compare(templateValue, new HashSet<>(Arrays.asList(c.value)), c.constType)) {
								models.add(new ConstraintPropertyModel(templateName, templateType.getResource(), 
										c.property, "template value: " + templateValue + " not greater or equal than constraint definition: " + c.value));
							}
						break;
						case LESS_OR_EQUAL:
							if (!ConstraintsEnum.LESS_OR_EQUAL.compare(templateValue, new HashSet<>(Arrays.asList(c.value)), c.constType)) {
								models.add(new ConstraintPropertyModel(templateName, templateType.getResource(), 
										c.property, "template value: " + templateValue + " not less or equal than constraint definition: " + c.value));	
							}
						break;
						case VALID_VALUES:
							LOG.info("valid_values: {}",  c.list);
							if (!ConstraintsEnum.VALID_VALUES.compare(templateValue, c.list, c.constType)) {
								models.add(new ConstraintPropertyModel(templateName, templateType.getResource(), 
										c.property, "template value: " + templateValue + " not included in valid values: " + c.list));	
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
		private String property;
		private String constraint;
		private String constType;
		private String value;
		private Set<String> list;
		
		ConstraintResult(String property, String constraint, String constType, String value) {
			this.property = property;
			this.constraint = constraint;
			this.constType = constType;
			this.value = value;
		}
		
		ConstraintResult(String property, String constraint, String constType, Set<String> list) {
			this.property = property;
			this.constraint = constraint;
			this.constType = constType;
			this.list = list;
		}
	}
}
