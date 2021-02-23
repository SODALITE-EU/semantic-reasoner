package tosca.mapper.exchange.generator;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tosca.mapper.dto.Node;
import tosca.mapper.dto.Property;
import tosca.mapper.exchange.generator.consts.ExchangeConsts;

public class AADMGenerator extends ModelGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(AADMGenerator.class.getName());
	Set<Node> nodes;
	Set<Property> inputs;
	
	public AADMGenerator(Set<Node> nodes, Set<Property> inputs) {
		this.nodes = nodes;
		this.inputs = inputs;
	}
	
	public void convertModelToExchange() {
		
		exchangeModel = exchangeModel.concat(ExchangeConsts.AADM_PREFIXES)
					.concat(ExchangeConsts.EXCHANGE_PREFIXES)
					.concat(ExchangeConsts.ONTOLOGY)
					.concat(ExchangeConsts.AADM_MODEL);
		
		convertConceptToExchange(inputs, ExchangeConsts.INPUT_PREFIX);
		
		nodes.forEach((n) -> {
			LOG.info("node name = {}", n.getName());
			LOG.info("node type = {}", n.getOfType());
			
			exchangeTypeModel += ExchangeConsts.TEMPLATE_PREFIX + node_template_counter++ + "\n";
			exchangeTypeModel += ExchangeConsts.EXCHANGE_RDF_TEMPLATE;
			exchangeTypeModel += ExchangeConsts.EXCHANGE_NAME + " \"" + n.getName() + "\" ;\n";
			exchangeTypeModel += ExchangeConsts.EXCHANGE_TYPE + " \"" + n.getOfType() + "\" ;\n";
			
			if (n.getDescription() != null)
				exchangeTypeModel += ExchangeConsts.EXCHANGE_DESCRIPTION + " \'" + n.getDescription().replaceAll("'", "\\\\'").replaceAll("[\\n\\r]+","\\\\n") + "\' ;\n";
			
			assignConceptsToModel(n);
		});
		exchangeModel += exchangeTypeModel + exchangeConceptModel ;
		LOG.info("EXCHANGE MODEL: {}", exchangeModel);
	}
}
