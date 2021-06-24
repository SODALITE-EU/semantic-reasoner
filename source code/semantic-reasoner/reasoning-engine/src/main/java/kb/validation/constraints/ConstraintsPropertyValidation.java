package kb.validation.constraints;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.dsl.utils.ErrorConsts;
import kb.dsl.utils.NamedResource;
import kb.dto.PropertyMap;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.models.ConstraintPropertyModel;

public class ConstraintsPropertyValidation {
	private static final Logger LOG = LoggerFactory.getLogger(ConstraintsPropertyValidation.class.getName());
	private String kindOfTemplate;
	private String templateName;
	private NamedResource templateType;
	private Map<String, String> exchangePropertyValues;
	private List<PropertyMap> exchangePropertyMapValues;
	private KB kb;
	private static final String SLASH = "/";
	
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
	
	public ConstraintsPropertyValidation(String kindOfTemplate, String templateName, NamedResource templateType, Map<String, String> exchangePropertyValues, List<PropertyMap> exchangePropertyMapValues, KB kb) {
		this.kindOfTemplate = kindOfTemplate;
		this.templateName = templateName;
		this.templateType = templateType;
		this.exchangePropertyValues = exchangePropertyValues;
		this.exchangePropertyMapValues = exchangePropertyMapValues;
		this.kb = kb;
	}

	public List<ConstraintPropertyModel> validate() throws IOException {
		String type = templateType.getResourceURI();
		if (type == null)
			type = KB.TOSCA + templateType.getResource();
		
		loadConstraintTypeSchema(type);
		validatePropertyValues();
		
		results.clear();
		
		loadConstraintTypeSchemaForMaps(type);
		validatePropertyMapValues();
		
		
		return models;
	}
	
	public void loadConstraintTypeSchema(String type)  {
		String query = KB.PREFIXES + constraintsQuery;
		String listquery = KB.PREFIXES + getListValuesQuery;
		
		
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
		
	}
	
	public ConstraintResult loadConstraintTypeSchemaForProperty(String type, String property) throws IOException  {
		LOG.info("loadConstraintTypeSchemaForProperty type: {}, property: {}", type, property);
		ConstraintResult c = null;
		
		String sparql = MyUtils.fileToString("sparql/validation/constraints/constraintsDefProperty.sparql");
		String query = KB.PREFIXES + sparql;
		
		String listquery = KB.PREFIXES + getListValuesQuery;
		
		
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] { new SimpleBinding("var", kb.factory.createIRI(type)),
				new SimpleBinding("var_property", kb.getFactory().createLiteral(property))});
		
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			String constr = MyUtils.getStringValue(bindingSet.getBinding("constraint").getValue());
			String constrType = bindingSet.getBinding("constr_type").getValue().stringValue();
			String typeValue = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue().stringValue() : null;
			IRI typeList = bindingSet.hasBinding("listvalue") ? (IRI) bindingSet.getBinding("listvalue").getValue() : null;

			
			//get list query
			if (typeList != null) {
				TupleQueryResult result2 = QueryUtil.evaluateSelectQuery(kb.getConnection(), listquery, new SimpleBinding("var", typeList));
				Set<String> list = new HashSet<>();
				while(result2.hasNext()) {
					BindingSet bindingSet2 = result2.next();
					String value = MyUtils.getStringValue(bindingSet2.getBinding("value").getValue());
					list.add(value);
				}
				c = new ConstraintResult(property, constr, constrType, list);
			} else {
				c = new ConstraintResult(property, constr, constrType, typeValue);
			}
			LOG.info("loadConstraintTypeSchemaForProperty constrType: {}, constraint: {}, typeValue: {}, typeList: {}", constrType, constr, typeValue, typeList);
		}
		
		return c;
	}

	public void loadConstraintTypeSchemaForMaps(String type) throws IOException {
		String queryForMap = KB.PREFIXES + MyUtils.fileToString("sparql/validation/constraints/constraintsDefPropertyMap.sparql");
		TupleQueryResult result2 = QueryUtil.evaluateSelectQuery(kb.getConnection(), queryForMap, new SimpleBinding("var", kb.factory.createIRI(type)));
		//Check Map values
		while(result2.hasNext()) {
			
			BindingSet bindingSet = result2.next();
			String prop = MyUtils.getStringValue(bindingSet.getBinding("property").getValue());
			String constr = MyUtils.getStringValue(bindingSet.getBinding("constraint").getValue());
			String constrType = bindingSet.getBinding("constr_type").getValue().stringValue();
			String typeValue = bindingSet.hasBinding("value") ? bindingSet.getBinding("value").getValue().stringValue() : null;
			String entrySchema = bindingSet.getBinding("entry_schema").getValue().stringValue();
			
			ConstraintResult c = new ConstraintResult(prop, constr, constrType, typeValue, entrySchema);
			LOG.info("Property Map, constrType: {}, constraint: {}, property: {}, entrySchema: {}", constrType, constr, prop, entrySchema);
			
			results.add(c);
		}
	}
	
	public void validatePropertyValues() {
		for (Map.Entry<String, String> e : exchangePropertyValues.entrySet()) {	
			for (ConstraintResult c: results) {
				if (e.getKey().equals(c.property)) {
					checkConstraints(e.getValue(),  c, c.property);
				}
			}
		}
	}
	
	public void validatePropertyMapValues() {
		exchangePropertyMapValues.forEach((p) -> {
			LOG.info("exchangePropertyMapValues, Name: {}, getMapProperties: {}", p.getName(), p.getMapProperties());
			for (ConstraintResult c: results) {
				if (p.getName().equals(c.property)) {
					int numOfMaps = p.getMapProperties().size(); 
					ConstraintsEnum constraint = ConstraintsEnum.valueOf(c.constraint.toUpperCase());
					switch (constraint) {
						case MIN_LENGTH:
							if (!ConstraintsEnum.MIN_LENGTH.compare(String.valueOf(numOfMaps) , new HashSet<>(Arrays.asList(c.value)), c.constType)) {
								models.add(new ConstraintPropertyModel(getBaseContextPath() + c.property , templateType.getResource(), 
										c.property, c.property, "Map count: " + numOfMaps + " not greater or equal than MIN_LENGTH constraint definition: " + c.value));
							}
						break;
						case LENGTH:
							if (!ConstraintsEnum.LENGTH.compare(String.valueOf(numOfMaps) , new HashSet<>(Arrays.asList(c.value)), c.constType)) {
								models.add(new ConstraintPropertyModel(getBaseContextPath() + c.property, templateType.getResource(), 
										c.property, c.property, "Map count: " + numOfMaps + " not equal LENGTH constraint definition: " + c.value));
							}
						break;
						case MAX_LENGTH:
							if (!ConstraintsEnum.MAX_LENGTH.compare(String.valueOf(numOfMaps) , new HashSet<>(Arrays.asList(c.value)), c.constType)) {
								models.add(new ConstraintPropertyModel(getBaseContextPath() + c.property, templateType.getResource(), 
										c.property, c.property, "Map count: " + numOfMaps + "  greater than MAX_LENGTH constraint definition: " + c.value));
							}
						break;
						default:
							break;
					}
					
					//Retrieve type schema for properties with type: map, entry_schema: type
					LOG.info("Entry Schema: {}", c.entrySchema);
					//TAKE THE SCHEMA OF c.entrySchema ASSIGN TO ConstraintResult
					HashMap<String, HashMap<String, String>> mapProperties = p.getMapProperties();
					Iterator it = mapProperties.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry pair = (Entry) it.next();
						String subParameter = (String) pair.getKey();
						HashMap<String, String> subProperties = (HashMap<String, String>) pair.getValue();
						
						Iterator itSub = subProperties.entrySet().iterator();
						while (itSub.hasNext()) {
							Map.Entry subPair = (Entry) itSub.next();
							String subSubParameter = subPair.getKey().toString();
							
							ConstraintResult typeSchema = null;
							try {
								typeSchema = loadConstraintTypeSchemaForProperty(c.entrySchema, subSubParameter);
							} catch (IOException e) {
								e.printStackTrace();
							}
							
							if (typeSchema != null)
								checkConstraints(subPair.getValue().toString(), typeSchema, p.getName() + SLASH + subParameter + SLASH + subSubParameter);
						}
					}
					
				}
			}
		});
	}
	
	//check constraints for normal properties - that are not of type:map
	public void checkConstraints(String templateValue, ConstraintResult c, String path) {
		ConstraintsEnum constraint = ConstraintsEnum.valueOf(c.constraint.toUpperCase());
		switch (constraint) {
			case GREATER_OR_EQUAL:
				if (!ConstraintsEnum.GREATER_OR_EQUAL.compare(templateValue, new HashSet<>(Arrays.asList(c.value)), c.constType)) {
					models.add(new ConstraintPropertyModel(getBaseContextPath()  + path, templateType.getResource(), 
							c.property, path, "template value: " + templateValue + " not greater or equal than constraint definition: " + c.value));
				}
			break;
			case LESS_OR_EQUAL:
				if (!ConstraintsEnum.LESS_OR_EQUAL.compare(templateValue, new HashSet<>(Arrays.asList(c.value)), c.constType)) {
					models.add(new ConstraintPropertyModel(getBaseContextPath() + path, templateType.getResource(), 
							c.property, path,"template value: " + templateValue + " not less or equal than constraint definition: " + c.value));	
				}
			break;
			case VALID_VALUES:
				LOG.info("valid_values: {}",  c.list);
				if (!ConstraintsEnum.VALID_VALUES.compare(templateValue, c.list, c.constType)) {
					models.add(new ConstraintPropertyModel(getBaseContextPath() + path, templateType.getResource(), 
							c.property, path, "template value: " + templateValue + " not included in valid values: " + c.list));	
				}
			break;
			default:
				break;
		}
	}
	
	private String getBaseContextPath() {
		return kindOfTemplate + KBConsts.SLASH + templateName +  KBConsts.SLASH + KBConsts.PROPERTIES + KBConsts.SLASH;
	}
	
	private class ConstraintResult {
		private String property;
		private String constraint;
		private String constType;
		private String value;
		private Set<String> list;
		//for nested map types
		private String entrySchema;
		
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
		
		ConstraintResult(String property, String constraint, String constType, String value, String entrySchema) {
			this.property = property;
			this.constraint = constraint;
			this.constType = constType;
			this.value = value;
			this.entrySchema = entrySchema;
		}
	}
}
