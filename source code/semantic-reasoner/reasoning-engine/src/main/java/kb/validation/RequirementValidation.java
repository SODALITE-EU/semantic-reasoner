package kb.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.dsl.verify.singularity.VerifySingularity;
import kb.repository.KB;
import kb.utils.InferencesUtil;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;
import kb.validation.exceptions.models.RequiredPropertyAttributeModel;
import kb.validation.exceptions.models.RequirementModel;


/*
 * THIS NEEDS TO BE CHANGED IN ORDER TO PERFORM VALIDATION ON THE MODEL, BEFORE SAVING IT TO THE KB
 */

public class RequirementValidation extends ValidationManager {
	private static final Logger LOG = LoggerFactory.getLogger(RequirementValidation.class.getName());
	String aadmId;
	Set<HashMap<String, IRI>> templateRequirements;
	HashMap<IRI, IRI> templateTypes;
	
	List<RequirementModel> models = new ArrayList<RequirementModel>();
	
	public RequirementValidation(Model model) {
		super(model);
	}

	public RequirementValidation(String aadmId, Set<HashMap<String, IRI>> templateRequirements, HashMap<IRI, IRI> templateTypes, KB kb) {
		super(kb);
		this.aadmId = aadmId;
		this.templateRequirements = templateRequirements;
		this.templateTypes = templateTypes;
	}
	//public List<RequirementModel> start() throws IOException, NoRequirementDefinitionValidationException,
		//	NodeMismatchValidationException, CapabilityMismatchValidationException {
		
	public List<RequirementModel> start() throws IOException {
		LOG.info("Sommelier Validations for requirements aadmId = {}", aadmId);
		LOG.info("templateTypes = {}", templateTypes.toString());
		 
		/*Set<HashMap<String, IRI>> templateRequirements = getTemplateRequirements();*/
		if (templateRequirements.isEmpty()) {
			LOG.info("No requirements to validate");
			return models;
			//return true;
		}
		for (HashMap<String, IRI> templateRequirement : templateRequirements) {// template, templateType, r_a
			IRI template = templateRequirement.get("template");
			IRI r_a = templateRequirement.get("r_a");
			IRI node = templateRequirement.get("node");
			IRI capability = templateRequirement.get("capability");
			
			System.err.println("type template = " + node);
			
			LOG.info("RequirementValidation {}, r_a = {}", templateRequirement.get("template").toString(), r_a.toString());
			// 1.1 + 1.2 (check if rd.node exists)
			boolean hasRequirementDefinition = requirementDefinitions(templateRequirement);
			
			if (hasRequirementDefinition == false) {
				//throw new NoRequirementDefinitionValidationException(template, r_a);
				models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a),"NoRequirementDefinition"));
			}

			HashMap<String, IRI> requirementDefinition = getRequirementContexts(templateRequirement);
			
			//for (HashMap<String, IRI> requirementDefinition : requirementDefinitions) { // ?nodeType ?ctx
			if (requirementDefinition != null) {	
			
				IRI nodeCtx = requirementDefinition.get("nodeContext");
				IRI nodeType = requirementDefinition.get("nodeType");

				// 1.2
				if(nodeCtx != null && node != null) {
					IRI typeOfNode = getTypeofTemplate(node);
					HashMap<String, IRI> nodeMismatch = nodeMismatch(nodeCtx, r_a, typeOfNode);
			
					LOG.info("template = {}, r_a = {}, nodeCtx = {}", template, r_a, nodeCtx);
					if (!nodeMismatch.isEmpty()) {
						//throw new NodeMismatchValidationException(nodeMismatch.get("type_r_a_node"), template,
							//nodeMismatch.get("r_d_node"));
						models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a) + "/node", typeOfNode.toString(), MyUtils.getStringValue(nodeMismatch.get("r_d_node")), "NodeMismatch"));		
					}
				}

				// ra.capability exists (1.3)
				IRI capCtx = requirementDefinition.get("capContext");
				if (capCtx != null) {
					if (capability != null) {
						IRI typeOfCapability = getTypeofTemplate(capability);
						HashMap<String, IRI> capabilityExistsMismatch = capabilityExistsMismatch(this.templateTypes.get(capability), r_a, capCtx);
						if (!capabilityExistsMismatch.isEmpty()) {
							models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a) + "/capability", typeOfCapability.toString(), MyUtils.getStringValue(capabilityExistsMismatch.get("r_d_capability")),  MyUtils.getStringValue(nodeType), "CapabilityExistsMismatch"));		
						}
					}
				
					// ra.capability not exists (1.4)
					if (node != null) {
						IRI typeOfNode = getTypeofTemplate(node);
						HashMap<String, IRI> capabilityMatch = capabilityMatch(typeOfNode, r_a, capCtx);
						if (!capabilityMatch.isEmpty()) {
							/*throw new CapabilityMismatchValidationException(template, r_a, nodeType,
								capabilityMatch.get("templateCapabilityType"), capabilityMatch.get("r_d_capability"));*/
							models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a), typeOfNode.toString(), MyUtils.getStringValue(capabilityMatch.get("r_d_capability")),  MyUtils.getStringValue(nodeType), "CapabilityMismatch"));		
						}
					}
				}
				
				/*IRI relCtx = requirementDefinition.get("relContext");
				// ra.relationship exists (1.5)
				if (relCtx != null) {
					HashMap<String, IRI> relationshipMismatch = relationshipMisMatch(template, r_a, relCtx);
					if (!relationshipMismatch.isEmpty()) {
						models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a), MyUtils.getStringValue(relationshipMismatch.get("templateRelationshipType")), MyUtils.getStringValue(relationshipMismatch.get("r_d_relationship")),  MyUtils.getStringValue(nodeType), "RelationshipMismatch"));		
					}
				}*/

			}

		}

		return models;
		//return true;
	}
	
	
	// Sommelier 1.3
	private HashMap<String, IRI> capabilityExistsMismatch(IRI templateCapabilityType, IRI r_a, IRI ctx) throws IOException {
		LOG.info("capabilityExistsMismatch: templateCapabilityType:{}, r_a:{}, ctx:{}", templateCapabilityType, r_a.getLocalName(), ctx.getLocalName());

		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/capabilityExistsViolation.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("templateCapabilityType", templateCapabilityType),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI r_d_capability = (IRI) bindingSet.getBinding("r_d_capability").getValue();

			r.put("r_d_capability", r_d_capability);

		}
		result.close();
		return r;

	}

	// Sommelier 1.4
	private HashMap<String, IRI> capabilityMatch(IRI type_r_a_node, IRI r_a, IRI ctx) throws IOException {
		LOG.info("capabilityNotExistsCheck: type_r_a_node:{}, r_a:{}, ctx:{}", type_r_a_node.getLocalName(), r_a.getLocalName(), ctx.getLocalName());

		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/capabilityNotExistsCheck.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("type_r_a_node", type_r_a_node),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI r_d_capability = (IRI) bindingSet.getBinding("r_d_capability").getValue();

			r.put("r_d_capability", r_d_capability);

		}
		result.close();
		return r;

	}

	// Sommelier 1.2
	private HashMap<String, IRI> nodeMismatch(IRI ctx, IRI r_a, IRI type_r_a_node) throws IOException {
		LOG.info("nodeMismatch: ctx:{}, r_a:{}, type_r_a_node:{}",  ctx.getLocalName(),
				r_a.getLocalName(), type_r_a_node);
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/nodeMismatch.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("type_r_a_node", type_r_a_node),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI r_d_node = (IRI) bindingSet.getBinding("r_d_node").getValue();

			r.put("r_d_node", r_d_node);
		}
		result.close();
		return r;

	}
	
	// Sommelier 1.5
	private HashMap<String, IRI> relationshipMisMatch(IRI template, IRI r_a, IRI ctx) throws IOException {
		LOG.info("capabilityExistsMismatch: template:{}, r_a:{}, ctx:{}", template.getLocalName(),
				r_a.getLocalName(), ctx.getLocalName());

		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/relationshipMismatch.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("template", template),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI templateCapabilityType = (IRI) bindingSet.getBinding("templateRelationshipType").getValue();
			IRI r_d_capability = (IRI) bindingSet.getBinding("r_d_relationship").getValue();

			r.put("templateRelationshipType", templateCapabilityType);
			r.put("r_d_relationship", r_d_capability);

		}
		result.close();
		return r;

	}

	public Set<HashMap<String, IRI>> getTemplateRequirements() throws IOException {
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/getAllTemplateRequirements.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("aadmId", kb.getFactory().createLiteral(aadmId)));

		Set<HashMap<String, IRI>> container = new HashSet<>();

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI template = (IRI) bindingSet.getBinding("template").getValue();
			IRI templateType = (IRI) bindingSet.getBinding("templateType").getValue();
			IRI r_a = (IRI) bindingSet.getBinding("r_a").getValue();

			HashMap<String, IRI> templateRequirements = new HashMap<>();
			templateRequirements.put("template", template);
			templateRequirements.put("templateType", templateType);
			templateRequirements.put("r_a", r_a);

			container.add(templateRequirements);
		}
		result.close();
		return container;
	}

	/*Get requirements node, capability, and relationship context separately so as to return the lowest context in the
	*hierarchy for each requirement. e.g. requirement/host/node might be defined in many node types in the hierarchy*/
	public HashMap<String, IRI> getRequirementContexts(HashMap<String, IRI> dto) throws IOException {
		HashMap<String, IRI> contexts = new HashMap<String, IRI>();
		IRI nodeType = dto.get("templateType");
		contexts.put("nodeType", nodeType);
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/getReqNodeContext.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] {
				new SimpleBinding("r_a", dto.get("r_a")), new SimpleBinding("nodeType", nodeType) });
		contexts.put("nodeContext", getLowestContext(result));
		result.close();
		
		query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/getReqCapContext.sparql");
		result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] {
				new SimpleBinding("r_a", dto.get("r_a")), new SimpleBinding("nodeType", nodeType) });
		contexts.put("capContext", getLowestContext(result));
		result.close();
		
		query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/getReqRelContext.sparql");
		result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] {
				new SimpleBinding("r_a", dto.get("r_a")), new SimpleBinding("nodeType", nodeType) });
		contexts.put("relContext", getLowestContext(result));
		result.close();
		
		
		return contexts;
	}
	
	
	public IRI getTypeofTemplate(IRI template) {
		IRI type = this.templateTypes.get(template);
		
		if (type != null)
			return type;
		// query should be modified so as FILTER not to be needed
		String sparql = "SELECT ?type \r\n"
				+ "WHERE {\r\n"
				+ "\t?var a soda:SodaliteSituation;\r\n"
				+ "\tsesame:directType ?type;\r\n"
				+ "\tsoda:hasContext ?c;\r\n"
				+ "\tFILTER (?type != rdfs:Resource && ?type != DUL:Region)\r\n"
				+ "}";
		String query = KB.PREFIXES + sparql;
		
		LOG.info("getTypeofTemplate: {}", query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("var", template));
		
		IRI t = null;
		if (result.hasNext()) {
			BindingSet bindingSet = result.next();
			t = (IRI) bindingSet.getBinding("type").getValue();
		}
		result.close();
		
		return t;
	}
	
	
	public IRI getLowestContext(TupleQueryResult result) {
		Set<HashMap<String, IRI>> container = new HashSet<>();
		Set<IRI> resources = new HashSet<IRI>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI ctx = (IRI) bindingSet.getBinding("ctx").getValue();
			IRI resource = (IRI) bindingSet.getBinding("resource").getValue();
			
			HashMap<String, IRI> nodeTypeRequirements = new HashMap<String, IRI>();
			
			nodeTypeRequirements.put("ctx", ctx);
			nodeTypeRequirements.put("resource", resource);
			
			resources.add(resource);
			
			container.add(nodeTypeRequirements);
		}
		result.close();
		
		
		IRI lowestR_AContext = null;
		if (container.size() > 1) {
			IRI lowestNode = InferencesUtil.getLowestSubclass(kb, resources);
			
			for (HashMap<String, IRI> req: container) {
				IRI resource = req.get("resource");
				if (resource.equals(lowestNode)) {
					lowestR_AContext = req.get("ctx");
					break;
				}
					
			}
		} else if (container.size() == 1) {
			lowestR_AContext = container.iterator().next().get("ctx");
		}
		
		return lowestR_AContext;
	}
	
	// Sommelier 1.1
	public boolean requirementDefinitions(HashMap<String, IRI> dto) throws IOException {	
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/requirementDefinitionExists.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] {
				new SimpleBinding("r_a", dto.get("r_a")), new SimpleBinding("nodeType", dto.get("templateType")) });

		return result.hasNext();
	}

	public boolean condition1_1() {
		return true;
	}

	public static void main(String[] args) throws IOException {
	//	RequirementValidation requirementValidation = new RequirementValidation(null);

	/*	try {
			boolean start = requirementValidation.start();
			System.out.println(start);
			requirementValidation.shutDown();
		} catch (NoRequirementDefinitionValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NodeMismatchValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CapabilityMismatchValidationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (requirementValidation != null)
				requirementValidation.shutDown();
		}*/
	}

}
