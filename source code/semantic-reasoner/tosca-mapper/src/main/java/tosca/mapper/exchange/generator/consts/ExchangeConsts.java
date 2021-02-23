package tosca.mapper.exchange.generator.consts;

import java.util.HashMap;
import java.util.Map;

public final class ExchangeConsts {
	public final static String RM_PREFIXES = "# baseURI: https://www.sodalite.eu/ontologies/exchange/rm/\r\n" + 
			"# imports: https://www.sodalite.eu/ontologies/exchange/\r\n" + 
			"\r\n" + 
			"@prefix : <https://www.sodalite.eu/ontologies/exchange/rm/> .\r\n";
	public final static String AADM_PREFIXES = "# baseURI: https://www.sodalite.eu/ontologies/exchange/aadm/\r\n" + 
			"# imports: https://www.sodalite.eu/ontologies/exchange/\r\n" + 
			"\r\n" + 
			"@prefix : <https://www.sodalite.eu/ontologies/exchange/aadm/> .\r\n";
	
			
	public final static String EXCHANGE_PREFIXES = "@prefix exchange: <https://www.sodalite.eu/ontologies/exchange/> .\r\n" + 
			"@prefix owl: <http://www.w3.org/2002/07/owl#> .\r\n" + 
			"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\r\n" + 
			"@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .\r\n" + 
			"@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\r\n" + 
			"\r\n";
			
	public final static String ONTOLOGY = ":\r\n" + 
			"  rdf:type owl:Ontology ;\r\n" + 
			"  owl:imports exchange: ;\r\n" + 
			"  owl:versionInfo \"Created by the SODALITE IDE\" ;\r\n" + 
			".\n";
	public final static String RM_MODEL = ":RM_1\r\n" + 
			"  rdf:type exchange:RM ;\r\n" + 
			"  exchange:userId \"27827d44-0f6c-11ea-8d71-362b9e155667\" ;\r\n" + 
			".\r\n\n";
	public final static String AADM_MODEL = ":AADM_1\r\n" + 
			"  rdf:type exchange:AADM ;\r\n" + 
			"  exchange:userId \"27827d44-0f6c-11ea-8d71-362b9e155667\" ;\r\n" + 
			".\r\n";
	
	public final static String ATTRIBUTE_PREFIX = ":Attribute_";
	public final static String PARAMETER_PREFIX = ":Parameter_";
	public final static String PROPERTY_PREFIX = ":Property_";
	public final static String REQUIREMENT_PREFIX = ":Requirement_";
	public final static String CAPABILITY_PREFIX = ":Capability_";
	public final static String TYPE_PREFIX = ":Type_";
	public final static String TEMPLATE_PREFIX = ":Template_";
	public final static String INPUT_PREFIX = ":Input_";
	public final static String EXCHANGE_RDF_TYPE = "  rdf:type exchange:Type ;\n";
	public final static String EXCHANGE_RDF_TEMPLATE = "  rdf:type exchange:Template ;\n";
	public final static String EXCHANGE_TYPE = "  exchange:type ";
	public final static String EXCHANGE_NAME = "  exchange:name ";
	public final static String EXCHANGE_DERIVES_FROM = "  exchange:derivesFrom ";
	public final static String EXCHANGE_VALUE = "  exchange:value ";
	public final static String EXCHANGE_HAS_PARAMETER = "  exchange:hasParameter ";
	
	public final static String EXCHANGE_ATTRIBUTES = "  exchange:attributes";
	public final static String EXCHANGE_PROPERTIES = "  exchange:properties";
	public final static String EXCHANGE_CAPABILITIES = "  exchange:capabilities";
	public final static String EXCHANGE_REQUIREMENTS = "  exchange:requirements";
	public final static String EXCHANGE_DESCRIPTION = "  exchange:description";
	
	public final static String RDF_TYPE = "  rdf:type ";
	
	
	
	public static final Map<String,String> CONCEPTS = new HashMap<String, String>();
	static {
		CONCEPTS.put(PROPERTY_PREFIX, "  rdf:type exchange:Property ;\n");
		CONCEPTS.put(PARAMETER_PREFIX, "  rdf:type exchange:Parameter ;\n");
		CONCEPTS.put(REQUIREMENT_PREFIX, "  rdf:type exchange:Requirement ;\n");
		CONCEPTS.put(CAPABILITY_PREFIX, "  rdf:type exchange:Capability ;\n");
		CONCEPTS.put(INPUT_PREFIX, "  rdf:type exchange:Input ;\n");
		CONCEPTS.put(ATTRIBUTE_PREFIX, "  rdf:type exchange:Attribute ;\n");
	}
		
}
