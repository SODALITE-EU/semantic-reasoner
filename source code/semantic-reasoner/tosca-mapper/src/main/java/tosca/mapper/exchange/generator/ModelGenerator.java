package tosca.mapper.exchange.generator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tosca.mapper.dto.Node;
import tosca.mapper.dto.Parameter;
import tosca.mapper.exchange.generator.consts.ExchangeConsts;

public class ModelGenerator extends ExchangeGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(ModelGenerator.class.getName());
	//final model is here
	String exchangeModel = "";
	//templates or types
	String exchangeTypeModel = "";
	//properties, attributes e.t.c
	String exchangeConceptModel = "";
	
	
	boolean aadm;
	
	int node_type_counter = 1;
	int node_template_counter = 1;
	int property_counter = 1;
	int attribute_counter = 1;
	int requirement_counter = 1;
	int capability_counter = 1;
	int parameter_counter = 1;
	int input_counter = 1;
	
	public String getExchangeModel() {
		return this.exchangeModel;
	}
	
	public  Set<String> convertConceptToExchange(Set<? extends Parameter> concepts, String concept) {
		LOG.info("concepts: {}, concept: {}", concepts, concept);
		String exchangeOneConceptModel = "";
		String exchangeParameterModel = "";
		Set<String> exchangeConcepts = new HashSet<>();
		for (Parameter p : concepts) {
			LOG.info("{}: concept: {}", concept, p.getName());
			//Property_1, Parameter_1 e.t.c
			String exchangeConcept =  concept + getCounter(concept) ;
			exchangeConcepts.add(exchangeConcept);
			exchangeOneConceptModel += "\n" + exchangeConcept + "\n";
			exchangeOneConceptModel += ExchangeConsts.CONCEPTS.get(concept);
			exchangeOneConceptModel += ExchangeConsts.EXCHANGE_NAME + " \"" + p.getName() + "\" ;\n";
			
			if (p.getDescription() != null)
				exchangeOneConceptModel += ExchangeConsts.EXCHANGE_DESCRIPTION + " \'" + p.getDescription().replaceAll("'", "\\\\'").replaceAll("[\\n\\r]+","\\\\n") + "\' ;\n";
			
			if(p.getValue()==null && p.getValues()==null) {
				Set<String> exchangeParameters = new HashSet<>();
				for (Parameter par : p.getParameters()) {
					String exchangeParameter =  ExchangeConsts.PARAMETER_PREFIX + parameter_counter++;
					exchangeParameterModel += "\n" + exchangeParameter + "\n";
					exchangeParameterModel += ExchangeConsts.CONCEPTS.get(ExchangeConsts.PARAMETER_PREFIX);
					exchangeParameters.add(exchangeParameter);
					exchangeParameterModel += ExchangeConsts.EXCHANGE_NAME + " \"" + par.getName() + "\" ;\n";
					if (par.getValue() == null && par.getValues() == null) {
						LOG.info("par:{}", par.getName());
						Set<String> exchangeParameterConcepts = convertConceptToExchange(par.getParameters(), ExchangeConsts.PARAMETER_PREFIX);
						for (String name: exchangeParameterConcepts)
							exchangeParameterModel += ExchangeConsts.EXCHANGE_HAS_PARAMETER + " " + name + " ;\n";
						exchangeParameterModel += ".\n";
					} else if (par.getValues() != null) {
						List<String> values = par.getValues();
						for(String val : values) {
							LOG.info("listvalue: {}", val);
							exchangeParameterModel += ExchangeConsts.EXCHANGE_VALUE + "\'" + val + "\' ;\n";
						}
						exchangeParameterModel += ".\n";
					} else {
						LOG.info("par: {}, value: {}", par.getName(),  par.getValue());
						exchangeParameterModel += ExchangeConsts.EXCHANGE_VALUE + "\'" + par.getValue() + "\' ;\n.\n";
					}
					exchangeOneConceptModel +=  ExchangeConsts.EXCHANGE_HAS_PARAMETER + exchangeParameter + " ;\n";
				}
				exchangeOneConceptModel += ".\n";
			} else if (p.getValues() != null) {
				List<String> values = p.getValues();
				for(String val : values) {
					LOG.info("listvalue: {}", val);
					exchangeOneConceptModel += ExchangeConsts.EXCHANGE_VALUE + "\'" + val + "\' ;\n";
				}
				exchangeOneConceptModel += ".\n";
			} else {
				String value = p.getValue();
				LOG.info(" value: {}", value);
				exchangeOneConceptModel += ExchangeConsts.EXCHANGE_VALUE + "\'" + value + "\' ;\n";
				exchangeOneConceptModel += ".\n";
			}
		}
		exchangeConceptModel += exchangeOneConceptModel + exchangeParameterModel;
		return exchangeConcepts;
	}
	
	public void assignConceptsToModel(Node n) {
		Set<String> exchangeConcepts;
		exchangeConcepts = convertConceptToExchange(n.getAttributes(), ExchangeConsts.ATTRIBUTE_PREFIX);
		exchangeConcepts.forEach((name) -> {exchangeTypeModel += ExchangeConsts.EXCHANGE_ATTRIBUTES + " " + name + " ;\n";} );
		exchangeConcepts = convertConceptToExchange(n.getProperties(), ExchangeConsts.PROPERTY_PREFIX);
		exchangeConcepts.forEach((name) -> {exchangeTypeModel += ExchangeConsts.EXCHANGE_PROPERTIES + " " + name + " ;\n";} );
		exchangeConcepts = convertConceptToExchange(n.getCapabilities(), ExchangeConsts.CAPABILITY_PREFIX);
		exchangeConcepts.forEach((name) -> {exchangeTypeModel += ExchangeConsts.EXCHANGE_CAPABILITIES + " " + name + " ;\n";} );
		exchangeConcepts = convertConceptToExchange(n.getRequirements(), ExchangeConsts.REQUIREMENT_PREFIX);
		exchangeConcepts.forEach((name) -> {exchangeTypeModel += ExchangeConsts.EXCHANGE_REQUIREMENTS + " " + name + " ;\n";} );
		exchangeTypeModel += ".\n";
	}
	
	public int getCounter(String concept) {
		switch (concept) {
			case ExchangeConsts.ATTRIBUTE_PREFIX:
				return attribute_counter++;
			case ExchangeConsts.PROPERTY_PREFIX:
				return property_counter++;
			case ExchangeConsts.PARAMETER_PREFIX:
				return parameter_counter++;
			case ExchangeConsts.REQUIREMENT_PREFIX:
				return requirement_counter++;
			case ExchangeConsts.CAPABILITY_PREFIX:
				return capability_counter++;
			case ExchangeConsts.INPUT_PREFIX:
				return input_counter++;
			default:
				LOG.info("Incorrect counter");
				return 0;
		}
	}
}
