package tosca.mapper.exchange.generator;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tosca.mapper.dto.Node;
import tosca.mapper.exchange.generator.consts.ExchangeConsts;

public class RMGenerator extends ModelGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(RMGenerator.class.getName());
	Set<Node> nodes;
	
	public RMGenerator(Set<Node> nodes) {
		this.nodes = nodes;
	}
	
	public void convertModelToExchange() {
		
		exchangeModel = exchangeModel.concat(ExchangeConsts.RM_PREFIXES)
					.concat(ExchangeConsts.EXCHANGE_PREFIXES)
					.concat(ExchangeConsts.ONTOLOGY)
					.concat(ExchangeConsts.RM_MODEL);
		
		nodes.forEach((n) -> {
			LOG.info("node name = {}", n.getName());
			LOG.info("node type = {}", n.getOfType());
			
			exchangeTypeModel += ExchangeConsts.TYPE_PREFIX + node_type_counter++ + "\n";
			exchangeTypeModel += ExchangeConsts.EXCHANGE_RDF_TYPE;
			exchangeTypeModel += ExchangeConsts.EXCHANGE_NAME + " \"" + n.getName() + "\" ;\n";
			exchangeTypeModel += ExchangeConsts.EXCHANGE_DERIVES_FROM + " \"" + n.getOfType() + "\" ;\n";
			
			if (n.getDescription() != null)
				exchangeTypeModel += ExchangeConsts.EXCHANGE_DESCRIPTION + " \'" + n.getDescription().replaceAll("'", "\\\\'").replaceAll("[\\n\\r]+","\\\\n") + "\' ;\n";
			
			assignConceptsToModel(n);
		});
		exchangeModel += exchangeTypeModel + exchangeConceptModel ;
		LOG.info("EXCHANGE MODEL: {}", exchangeModel);
	}

}
