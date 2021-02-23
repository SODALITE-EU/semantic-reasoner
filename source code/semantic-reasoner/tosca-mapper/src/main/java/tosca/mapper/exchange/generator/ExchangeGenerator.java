package tosca.mapper.exchange.generator;

import java.util.Set;

import tosca.mapper.dto.Node;
import tosca.mapper.dto.Parameter;

public abstract class ExchangeGenerator {
	public abstract Set<String> convertConceptToExchange(Set<? extends Parameter> concepts, String concept);
	public abstract void assignConceptsToModel(Node n);
}
