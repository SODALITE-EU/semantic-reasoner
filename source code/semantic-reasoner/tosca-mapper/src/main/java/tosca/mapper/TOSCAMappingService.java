package tosca.mapper;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import tosca.mapper.dto.Attribute;
import tosca.mapper.dto.Capability;
import tosca.mapper.dto.Node;
import tosca.mapper.dto.Parameter;
import tosca.mapper.dto.Property;
import tosca.mapper.dto.Requirement;
import tosca.mapper.exchange.generator.AADMGenerator;
import tosca.mapper.exchange.generator.RMGenerator;



public class TOSCAMappingService {
	private static final Logger LOG = LoggerFactory.getLogger(TOSCAMappingService.class.getName());
	private class ParseError extends  RuntimeException {}

	Yaml yaml = new Yaml();
	String inputModel;
	
	Set<Node> nodes = new HashSet<>();
	Set<Node> nodeTemplates = new HashSet<>();
	Set<Property> inputs =  new HashSet<>();
	
	public TOSCAMappingService(String inputModel){
		this.inputModel = inputModel;
	}
	
	
	private void Expect(boolean guard) {
		if (!guard)
			throw new ParseError();
	}
	  
	public void parse() throws IOException {
		Map<?,?> map = (Map) yaml.load(inputModel);
		if (null == map) {
			throw new ParseError();
		}
		LOG.info("data: {}", map.toString());
		parseDocument(map);
	}
	 
	public void parseDocument(Map<?,?> map) {
		map.forEach((k, v) ->  {	            
	            LOG.info("k: {}, v: {}", k, v);
	            String key = k.toString();
	            switch (key) {
	            	case "tosca_definitions_version":
	            		Expect(v.equals("tosca_simple_yaml_1_3"));
	            		break;
	            	case "description":
	            		break;
	            	case "data_types":
	            		nodes.addAll(parseTypes(v, ":DataType_"));
	            		break;
	            	case "node_types":
	            		nodes.addAll(parseTypes(v, ":NodeType_"));
	            		break;
	            	case "capability_types":
	            		nodes.addAll(parseTypes(v, ":CapabilityType_"));
	            		break;
	            	case "relationship_types":
	            		nodes.addAll(parseTypes(v, ":RelationshipType_"));
	            		break;
	            	case "topology_template":
	            		Map<?,?> templatesMap =  (Map<?,?>) v;
	            		templatesMap.forEach((k2, v2) ->  {
	            			switch (k2.toString()) {
	            				case "node_templates":
	            					Set<Node> templatesPerCategory = parseTemplates(v2);
	            					nodeTemplates.addAll(templatesPerCategory);
	            					break;
	            				case "inputs":
	            					inputs = parseProperties(v2);
	            					break;
	            			}
	            		});
	            		break;
	            	default:
	            		throw new ParseError();
	            }
		 });		
		
	 }
	 
	public Set<Node> parseTypes(Object value, String classType) {
		Set<Node> nodes = new HashSet<>();
		LOG.info("parseTypes: " + classType);
		Map<?,?> typeMap =  (Map<?,?>) value;
		typeMap.forEach((k, v) -> {
				String name = k.toString();
				Node node = new Node(name, classType);
				Map<?,?> descriptionMap =  (Map<?,?>) v;
				descriptionMap.forEach((k2, v2) ->  {
	            	String key2 = k2.toString();
	            	String value2 = v2.toString();
					switch (key2) {
						case "derived_from":
							node.setOfType(value2);
							break;
						case "description":
							node.setDescription(value2);
							break;
						case "attributes":
							Set<Attribute> attributes = parseAttributes(v2);
							node.setAttributes(attributes);	
							break;
						case "properties":
							Set<Property> properties = parseProperties(v2);
							node.setProperties(properties);	
							break;
						case "requirements":
							Set<Requirement> requirements = parseRequirements(v2);
							node.setRequirements(requirements);	
							break;
						case "capabilities":
							Set<Capability> capabilities = parseCapabilities(v2);
							node.setCapabilities(capabilities);	
							break;
						default:
							throw new ParseError();
					}
				});
				nodes.add(node);
			});
			return nodes;
	}
	
	public Set<Node> parseTemplates(Object value) {
		Set<Node> nodes = new HashSet<>();
		LOG.info("parseTemplates");
		Map<?,?> typeMap =  (Map<?,?>) value;
		typeMap.forEach((k, v) -> {
				String name = k.toString();
				Node node = new Node(name);
				Map<?,?> descriptionMap =  (Map<?,?>) v;
				descriptionMap.forEach((k2, v2) ->  {
	            	String key2 = k2.toString();
	            	String value2 = v2.toString();
					switch (key2) {
						case "type":
							node.setOfType(value2);
							break;
						case "description":
							node.setDescription(value2);
							break;
						case "attributes":
							Set<Attribute> attributes = parseAttributes(v2);
							node.setAttributes(attributes);	
							break;
						case "properties":
							Set<Property> properties = parseProperties(v2);
							node.setProperties(properties);	
							break;
						case "requirements":
							Set<Requirement> requirements = parseRequirements(v2);
							node.setRequirements(requirements);	
							break;
						case "capabilities":
							Set<Capability> capabilities = parseCapabilities(v2);
							node.setCapabilities(capabilities);	
							break;
						default:
							throw new ParseError();
					}
				});
				nodes.add(node);
			});
			return nodes;
	}

	public  Set<Attribute> parseAttributes(Object map) {
		Set<Attribute> attributes = new HashSet<>();
		Map<?,?> attrMap =  (Map<?,?>) map;		 
		attrMap.forEach((k, v) -> {
			Attribute p = new Attribute(k.toString());
			//attributes.add(p);
			valueOrParameter(v, p, attributes); 
		});
		 
		return attributes;
	}
	
	public  Set<Property> parseProperties(Object map) {
		Set<Property> properties = new HashSet<>();
		Map<?,?> propMap =  (Map<?,?>) map;		 
		propMap.forEach((k, v) -> {
			Property p = new Property(k.toString());
			//properties.add(p);
			String description = null;
			if (v instanceof Map) {
				Map<String,String> vMap =  (Map<String,String>) v;
				if (vMap.containsKey("description"))
					p.setDescription(vMap.get("description"));
			}
			
			if (description == null)
				valueOrParameter(v, p, properties);
		});
		 
		return properties;
	}
	
	public  Set<Requirement> parseRequirements(Object map) {
		Set<Requirement> requirements = new HashSet<>();
		ArrayList<?> reqList = (ArrayList<?>)map;
		reqList.forEach((req) -> {
			Map<?,?> reqMap =  (Map<?,?>) req;		 
			reqMap.forEach((k, v) -> {
				Requirement p = new Requirement(k.toString());
				//requirements.add(p);
				valueOrParameter(v, p, requirements); 
			});
		});
		 
		return requirements;
	}
	
	public  Set<Capability> parseCapabilities(Object map) {
		Set<Capability> capabilities = new HashSet<>();
		Map<?,?> capMap =  (Map<?,?>) map;		 
		capMap.forEach((k, v) -> {
			Capability cap = new Capability(k.toString());
			//capabilities.add(cap);
			valueOrParameter(v, cap, capabilities); 
		});
		 
		return capabilities;
	}
	 
	public Set<Parameter>  parseParameters(Object map) {
		Set<Parameter> parameters = new HashSet<>();
		Map<?,?> paramMap =  (Map<?,?>) map;
		paramMap.forEach((k, v) -> {
			Parameter p = new Parameter(k.toString()); 
			valueOrParameter(v, p, parameters);
		});
		return parameters;
	}
	 
	public <T> void valueOrParameter(Object v, Parameter p, Set<T> concepts) {
		Set<Parameter> parameters = new HashSet<>();
		if (v instanceof Map) {
			p.setParameters(parseParameters(v));
		} else if (v instanceof ArrayList) {
			//constraints is a list
			ArrayList<?> list = (ArrayList<?>)v;
			LOG.info("ARRAYLIST: {} ",  v);
			list.forEach((l)-> {
				if (l instanceof Map) {
					
					Map<?,?> paramMap =  (Map<?,?>) l;
					LOG.info("MAP: {}", l);
					paramMap.forEach((key, val) -> {
						Parameter p2 = new Parameter(key.toString());
						if (val instanceof ArrayList) {
							p2.setValues((ArrayList<String>)val);
						} else {
							p2.setValue(val.toString());
						}
						parameters.add(p2);
					});
				}
			});
			p.setParameters(parameters);
		} else {
			p.setValue(v.toString());
			p.setParameters(parameters);
		}
		
		concepts.add((T) p);
	 }
	 

	public String getExchangeAADM() {
		if (!nodeTemplates.isEmpty()) {
			AADMGenerator aadm = new AADMGenerator(nodeTemplates, inputs);
			aadm.convertModelToExchange();
			return aadm.getExchangeModel();
		}
		return null;
	}
	
	public String getExchangeRM() {
		if (!nodes.isEmpty()) {
			RMGenerator rm = new RMGenerator(nodes);
			rm.convertModelToExchange();
			return rm.getExchangeModel();
		}
		return null;
	}
	
	public static void main(String [] args) throws IOException {		
		String hpc_torque = "tosca_definitions_version: tosca_simple_yaml_1_3\n" +
		"capability_types:\r\n" + 
		"\r\n" + 
		"  sodalite.capabilities.TestTorque.Queue:\r\n" + 
		"    derived_from: tosca.capabilities.Compute\r\n" + 
		"\r\n" + 
		"  sodalite.capabilities.TestTorque.WM:\r\n" + 
		"    derived_from: tosca.capabilities.Compute\r\n" + 
		"\r\n" + 
		"  sodalite.capabilities.TestTorque.JobResources:\r\n" + 
		"    derived_from: tosca.capabilities.Compute\r\n" + 
		"    properties:\r\n" + 
		"      gpus:\r\n" + 
		"        type: integer\r\n" + 
		"        required: true\r\n" + 
		"      cpus:\r\n" + 
		"        type: integer\r\n" + 
		"        required: true\r\n" + 
		"      memory:\r\n" + 
		"        type: integer\r\n" + 
		"        required: false\n\n"+
		"    requirements:\r\n" + 
		"      - wm:                   \r\n" + 
		"          node: sodalite.nodes.hpc.TestTorque.TorqueWM\r\n" + 
		"          capability: sodalite.capabilities.TestTorque.WM\r\n" + 
		"          relationship: tosca.relationships.AttachesTo\n\n" +
		"      - zoe: hpc-wm-torque-TestTorque-wm\r\n\n"+
		"topology_template:\r\n" +
		"  inputs:\r\n\n" +
		"    user:\r\n" +
		"      type: string\r\n" +
		"    key-location:\r\n" + 
		"      type: string\r\n" + 
		"  node_templates:\r\n" + 
		"     node-hpc-TestTorque-node-1.novalocal:\r\n" + 
		"       type: sodalite.nodes.hpc.TestTorque.TorqueNode\r\n" + 
		"       properties:\r\n" + 
		"         name: node-1.novalocal\r\n" +
		"     hpc-wm-torque-TestTorque-wm:\r\n" + 
		"       type: sodalite.nodes.hpc.TestTorque.TorqueWM\r\n" + 
		"       attributes:\r\n" + 
		"         public_address: sodalite-fe.hlrs.de\r\n" + 
		"         username: { get_input: user }\r\n" + 
		"         ssh-key: { get_input: key-location }\r\n" + 
		"       capabilities:    \r\n" + 
		"         resources:\r\n" + 
		"           gpus: 5 \r\n" + 
		"           cpus: 200\r\n" + 
		"           memory: 650687";
		TOSCAMappingService t = new TOSCAMappingService(hpc_torque);
		
		t.parse();
		t.getExchangeAADM();
		t.getExchangeRM();
	}
}
