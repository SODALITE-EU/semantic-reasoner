package kb.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.repository.KB;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.CapabilityMismatchValidationException;
import kb.validation.exceptions.NoRequirementDefinitionValidationException;
import kb.validation.exceptions.NodeMismatchValidationException;
import kb.validation.exceptions.models.RequiredPropertyAttributeModel;
import kb.validation.exceptions.models.RequirementModel;

///			System.out.println(MyUtils.getGson(true).toJson(requirementDefinitionExists));

/*
 * THIS NEEDS TO BE CHANGED IN ORDER TO PERFORM VALIDATION ON THE MODEL, BEFORE SAVING IT TO THE KB
 */

public class RequirementValidation extends ValidationManager {
	private static final Logger LOG = Logger.getLogger(RequirementValidation.class.getName());

	String aadmId;
	List<RequirementModel> models = new ArrayList<RequirementModel>();
	
	public RequirementValidation(Model model) {
		super(model);
	}

	public RequirementValidation(String aadmId) {
		this.aadmId = aadmId;
	}
	//public List<RequirementModel> start() throws IOException, NoRequirementDefinitionValidationException,
		//	NodeMismatchValidationException, CapabilityMismatchValidationException {
		
	public List<RequirementModel> start() throws IOException {
		LOG.log(Level.INFO, "Sommelier Validations for requirements aadmId = {0}", aadmId);
		Set<HashMap<String, IRI>> templateRequirements = getTemplateRequirements();
		if (templateRequirements.isEmpty()) {
			LOG.info("No requirements to validate");
			return models;
			//return true;
		}
		for (HashMap<String, IRI> templateRequirement : templateRequirements) {// template, templateType, r_a
			IRI template = templateRequirement.get("template");
			IRI r_a = templateRequirement.get("r_a");
			
			LOG.log(Level.INFO, "RequirementValidation {0}, r_a = {1}", new Object[] {templateRequirement.get("template").toString(), r_a.toString()});
			// 1.1 + 1.2 (check if rd.node exists)
			Set<HashMap<String, IRI>> requirementDefinitions = requirementDefinitions(templateRequirement);
			if (requirementDefinitions.isEmpty()) {
				//throw new NoRequirementDefinitionValidationException(template, r_a);
				models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a),"NoRequirementDefinition"));
			}

			for (HashMap<String, IRI> requirementDefinition : requirementDefinitions) { // ?nodeType ?ctx
				IRI ctx = requirementDefinition.get("ctx");
				IRI nodeType = requirementDefinition.get("nodeType");

				// 1.2
				HashMap<String, IRI> nodeMismatch = nodeMismatch(ctx, r_a, template);
				LOG.log(Level.INFO, "template = {0}, r_a = {1}, ctx = {2}", new Object[] {template, r_a, ctx});
				if (!nodeMismatch.isEmpty()) {
					//throw new NodeMismatchValidationException(nodeMismatch.get("type_r_a_node"), template,
							//nodeMismatch.get("r_d_node"));
					models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a), MyUtils.getStringValue(nodeMismatch.get("type_r_a_node")), MyUtils.getStringValue(nodeMismatch.get("r_d_node")), "NodeMismatch"));		
				}

				// ra.capability exists (1.3)
				// TODO
				HashMap<String, IRI> capabilityExistsMismatch = capabilityExistsMismatch(template, r_a, ctx);
				if (!capabilityExistsMismatch.isEmpty()) {
					models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a), MyUtils.getStringValue(capabilityExistsMismatch.get("templateCapabilityType")), MyUtils.getStringValue(capabilityExistsMismatch.get("r_d_capability")),  MyUtils.getStringValue(nodeType), "CapabilityExistsMismatch"));		
				}
				
				// ra.capability not exists (1.4)
				HashMap<String, IRI> capabilityMatch = capabilityMatch(template, r_a, ctx);
				if (!capabilityMatch.isEmpty()) {
					/*throw new CapabilityMismatchValidationException(template, r_a, nodeType,
							capabilityMatch.get("templateCapabilityType"), capabilityMatch.get("r_d_capability"));*/
					models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a), MyUtils.getStringValue(capabilityMatch.get("templateCapabilityType")), MyUtils.getStringValue(capabilityMatch.get("r_d_capability")),  MyUtils.getStringValue(nodeType), "CapabilityMismatch"));		
				}

				// ra.relationship exists (1.5)
				HashMap<String, IRI> relationshipMismatch = relationshipMisMatch(template, r_a, ctx);
				if (!relationshipMismatch.isEmpty()) {
					models.add(new RequirementModel(MyUtils.getStringValue(template), MyUtils.getStringValue(r_a), MyUtils.getStringValue(relationshipMismatch.get("templateRelationshipType")), MyUtils.getStringValue(relationshipMismatch.get("r_d_relationship")),  MyUtils.getStringValue(nodeType), "RelationshipMismatch"));		
				}

			}

		}

		return models;
		//return true;
	}
	
	
	
	private HashMap<String, IRI> capabilityExistsMismatch(IRI template, IRI r_a, IRI ctx) throws IOException {
		LOG.log(Level.INFO, "capabilityExistsMismatch: template:{0}, r_a:{1}, ctx:{2}", new Object[] {template.getLocalName(), r_a.getLocalName(), ctx.getLocalName()});

		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/capabilityExistsViolation.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("template", template),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI templateCapabilityType = (IRI) bindingSet.getBinding("templateCapabilityType").getValue();
			IRI r_d_capability = (IRI) bindingSet.getBinding("r_d_capability").getValue();

			r.put("templateCapabilityType", templateCapabilityType);
			r.put("r_d_capability", r_d_capability);

		}
		result.close();
		return r;

	}

	private HashMap<String, IRI> capabilityMatch(IRI template, IRI r_a, IRI ctx) throws IOException {
		LOG.log(Level.INFO, "capabilityNotExistsCheck: template:{0}, r_a:{1}, ctx:{2}", new Object[] {template.getLocalName(), r_a.getLocalName(), ctx.getLocalName()});

		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/capabilityNotExistsCheck.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("template", template),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI templateCapabilityType = (IRI) bindingSet.getBinding("templateCapabilityType").getValue();
			IRI r_d_capability = (IRI) bindingSet.getBinding("r_d_capability").getValue();

			r.put("templateCapabilityType", templateCapabilityType);
			r.put("r_d_capability", r_d_capability);

		}
		result.close();
		return r;

	}


	private HashMap<String, IRI> nodeMismatch(IRI ctx, IRI r_a, IRI template) throws IOException {
		LOG.log(Level.INFO, "nodeMismatch: ctx:{0}, r_a:{1}, template:{2}", new Object[] { ctx.getLocalName(),
				r_a.getLocalName(), template.getLocalName()});
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/nodeMismatch.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query,
				new SimpleBinding[] { new SimpleBinding("r_a", r_a), new SimpleBinding("template", template),
						new SimpleBinding("ctx", ctx) });

		HashMap<String, IRI> r = new HashMap<>();
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();

			IRI type_r_a_node = (IRI) bindingSet.getBinding("type_r_a_node").getValue();
			IRI r_d_node = (IRI) bindingSet.getBinding("r_d_node").getValue();

			r.put("type_r_a_node", type_r_a_node);
			r.put("r_d_node", r_d_node);

		}
		result.close();
		return r;

	}
	
	private HashMap<String, IRI> relationshipMisMatch(IRI template, IRI r_a, IRI ctx) throws IOException {
		LOG.log(Level.INFO, "capabilityExistsMismatch: template:{0}, r_a:{1}, ctx:{2}", new Object[] { template.getLocalName(),
				r_a.getLocalName(), ctx.getLocalName()});

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

	// template, templateType, r_a
	public Set<HashMap<String, IRI>> requirementDefinitions(HashMap<String, IRI> dto) throws IOException {
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/requirementDefinitionExists.sparql");
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding[] {
				new SimpleBinding("r_a", dto.get("r_a")), new SimpleBinding("nodeType", dto.get("templateType")) });

		Set<HashMap<String, IRI>> container = new HashSet<>();

		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI nodeType = (IRI) bindingSet.getBinding("nodeType").getValue();
			IRI ctx = (IRI) bindingSet.getBinding("ctx").getValue();

			HashMap<String, IRI> nodeTypeRequirements = new HashMap<String, IRI>();
			nodeTypeRequirements.put("nodeType", nodeType);
			nodeTypeRequirements.put("ctx", ctx);

			container.add(nodeTypeRequirements);
		}
		result.close();
		return container;
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
