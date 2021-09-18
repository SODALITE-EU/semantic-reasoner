package kb.validation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

import kb.dsl.utils.ErrorConsts;
import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.InferencesUtil;
import kb.utils.MyUtils;
import kb.utils.QueryUtil;
import kb.validation.exceptions.models.RequirementExistenceModel;
import kb.validation.utils.UpdateKB;

/** A service that checks for omitted required/optional requirements
 * e.g.
 * requirements
	protected_by:
				node: XXXXX
 * If complete is true, the model in KB is getting updated by adding a (required/optional) requirement with
 * the found matching node to the corresponding template.
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
public class RequirementExistenceValidation extends ValidationManager {
	private static final Logger LOG = LoggerFactory.getLogger(RequirementExistenceValidation.class.getName());
	
	boolean complete;
	IRI aadmURI;
	
	Set<HashMap<String, IRI>> templateRequirements;
	HashMap<IRI, IRI> templateTypes;
	HashMap<IRI, String> templateClasses;

	String ws;
	IRI context;
	
	List<RequirementExistenceModel> optionalModels = new ArrayList<RequirementExistenceModel>();
	List<RequirementExistenceModel> requiredModels = new ArrayList<RequirementExistenceModel>();
	List<RequirementExistenceModel> modifiedModels = new ArrayList<RequirementExistenceModel>();
	
	//keep the templates to be modified in a structure since the aadm is not save yet in KB
	Set<HashMap<String, IRI>> ModelsToBeModified = new HashSet<>();
	
	
		
	public RequirementExistenceValidation(IRI aadm, boolean complete, KB kb, String ws, IRI context, Set<HashMap<String, IRI>> templateRequirements, HashMap<IRI, IRI> templateTypes, HashMap<IRI, String> templateClasses) {
		super(kb);
		this.aadmURI = aadm;
		this.complete = complete;
		this.kb = kb;
		this.ws = ws;
		this.context = context;
		
		this.templateRequirements = templateRequirements;
		this.templateTypes = templateTypes;
		this.templateClasses = templateClasses;
	}
	
	public List<RequirementExistenceModel> getModifiedModels() {
		return this.modifiedModels;
	}

	public Set<HashMap<String, IRI>> getModelsToBeModified() {
		return this.ModelsToBeModified;
	}
	
	//Validate for omitted required requirements
	public List<RequirementExistenceModel> validate() throws IOException {		
			HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>>  map = getTemplatesWithNoRequirements(true);
			if (map.isEmpty()) {
				LOG.info("No required requirement is missing");
				return requiredModels;
			}
			setModels(map, true);
			return requiredModels;
	}
	
	//Check for optional omitted requirements
	public List<RequirementExistenceModel> getSuggestions() throws IOException {
			HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>> map  = getTemplatesWithNoRequirements(false);
			if (map.isEmpty()) {
				LOG.info("No optional requirement is missing");
				return optionalModels;
			}
			setModels(map, false);
			return optionalModels;
	}

	/**
	 * 
	 * @param map Nested hash maps having templates with omitted requirements
	 * grouped by template_name, r_a, r_i.
	 * A template, r_a, r_i might have more than one target node type. The lowest in the class hierarchy (more specific)
	 * is used.
	 * @param required true/false - models are set for required/optional omitted requirements.
	 * @throws IOException input/output issue
	 */
	public void setModels(HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>> map, boolean required) throws IOException {
		
		List<RequirementExistenceModel> models;

		for (Map.Entry e : map.entrySet()) {
			 LOG.info("Template: {}", e.getKey());
			 IRI template = (IRI) e.getKey();
			 HashMap<IRI,HashMap<IRI,Set<IRI>>> innerMap =  (HashMap<IRI,HashMap<IRI,Set<IRI>>>) e.getValue();
			 for (Map.Entry e2 : innerMap.entrySet()) {
				 LOG.info("r_a: {}", e2.getKey());
				 IRI r_a = (IRI) e2.getKey();
				 HashMap<IRI,Set<IRI>> in_inMap = (HashMap<IRI,Set<IRI>>)e2.getValue();
				 for (Map.Entry e3 : in_inMap.entrySet()) {
					 LOG.info("r_i: {}, types = {}", e3.getKey(), e3.getValue());
					 IRI r_i = (IRI) e3.getKey();
					 //Get the most specific type
					 IRI type = InferencesUtil.getLowestSubclass(kb, (Set<IRI>)e3.getValue());
					 models = required ? requiredModels : optionalModels;

					 Set<IRI> templates= selectCompatibleTemplates(type);
					 
					 String contextPath = templateClasses.get(template) + MyUtils.getStringValue(template) + KBConsts.SLASH + KBConsts.REQUIREMENTS + KBConsts.SLASH +  MyUtils.getStringValue(r_a) +  KBConsts.SLASH + "node";
					 if (templates.size() == 1) {
						 if (complete) {
							 //keep the templates to be modified in a structure since the aadm is not save yet in KB
							 HashMap<String, IRI> tempReq = new HashMap<String, IRI>();
							 
							 tempReq.put("template", template);
							 tempReq.put("r_i", r_i);
							 tempReq.put("r_a", r_a);
							 tempReq.put("matching_template", templates.iterator().next());
							 this.ModelsToBeModified.add(tempReq);
							 
							 
							 
							 modifiedModels.add(new RequirementExistenceModel(contextPath, 
									 "The template " + MyUtils.getStringValue(template) + " does not declare a mandatory requirement and autocompleted by " + templates.iterator().next(), templates));
						 } else {
							 //models.add(new RequirementExistenceModel(template, r_a, r_i, templates));
							 String description;
							 if (required)
								 description = "The template " + MyUtils.getStringValue(template) + " does not declare a mandatory " + MyUtils.getStringValue(r_a) + " requirement" ;
							 else
								 description = "The template " + MyUtils.getStringValue(template) + " does not declare an optional" + MyUtils.getStringValue(r_a) + " requirement";
							 models.add(new RequirementExistenceModel(contextPath , description , templates));
						 }
					 }	else if (templates.isEmpty()){
						 if (required)
							 models.add(new RequirementExistenceModel(contextPath , "The template " + MyUtils.getStringValue(template) + " does not declare a mandatory " + MyUtils.getStringValue(r_a) + " requirement", templates));
					 } else {
						 // more than one template found
					 	models.add(new RequirementExistenceModel(contextPath, templates.size() + " matching target templates found for the " + MyUtils.getStringValue(r_a) + " requirement", templates));
					}	 
				 }
			 }
		}
		
	}
		

	/* *
	 * @param required flag. When true the required omitted requirements are retrieved (having min occurrences > 0),
	 * when false the optional requirements are returned
	 * @return HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>>
	 * Nested hash maps are used so as to group types based on template_name, r_a, r_i where e.g. r_a = host, r_i =  node corresponding to
	 * Set<IRI> [sodalite.nodes.DockerHost, tosca.nodes.Compute]. Finally the types will be eliminated to one (check setModels function), the lowest in hierarchy will
	 * be chosen.
	 * @throws IOException input/output issue
	 */
	public HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>> getTemplatesWithNoRequirements(boolean required) throws IOException {
		String fileName = required ? "missingRequiredRequirements.sparql" : "missingOptionalRequirements.sparql" ;
		String query = KB.PREFIXES + MyUtils.fileToString("sparql/validation/" + fileName);
		
		//HashMap<Template, HashMap<r_a, HashMap<r_inner, node types>>>
		//e.g. HashMap<snow-vm,HashMap<protected_by, HashMap<node, sodalite.nodes.OpenStack.SecurityRules>
		HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>> templatesWithNoRequirements = new HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>>();
		
		for (Map.Entry e : templateTypes.entrySet()) {
			IRI template = (IRI) e.getKey();
			IRI type = (IRI) e.getValue();
			
			TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("templateType", type));
			
			
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				IRI v = (IRI) bindingSet.getBinding("v").getValue();
				IRI r_a = (IRI) bindingSet.getBinding("r_a").getValue();
				IRI r_i = (IRI) bindingSet.getBinding("r_i").getValue();
				LOG.info("results : {} {} {} {}", template, v, r_a, r_i);
				Set <IRI> vList= new HashSet<IRI>();	
				vList.add(v);
				
				
				boolean reqAssignmentFound = false;
				
				/* For saving time, the aadm is not saved in KB, and this requirement existence validation is done before the model is saved
				 * So the template details are saved in java structures. Those java structures are also used for Sommelier validation.
				 * Here it is checked if the requirement assignement has been done.
				 */
				for (HashMap<String, IRI> templateRequirement : templateRequirements) {// template, templateType, r_a					
					if(template.toString().equals(templateRequirement.get("template").toString()) && r_a.toString().equals(templateRequirement.get("r_a").toString()) && templateRequirement.get("node") != null) {
						reqAssignmentFound = true;
						break;
					}
				}
				//Since the requirement assignement has been done, we have no violation
				if (reqAssignmentFound)
					continue;
				/* The code is generalized so all r_i to be checked, but in reality only node is supported according to our use case
				 * capability and relationship do not exist
				 */
				if (templatesWithNoRequirements.get(template)!= null) {
					if(templatesWithNoRequirements.get(template).get(r_a) != null) {
						if(templatesWithNoRequirements.get(template).get(r_a).get(r_i) != null)
							templatesWithNoRequirements.get(template).get(r_a).get(r_i).addAll(vList);
						else
							templatesWithNoRequirements.get(template).get(r_a).put(r_i, vList);
					} else {
						HashMap<IRI,Set<IRI>> in_inHash = new HashMap<IRI,Set<IRI>>();
						in_inHash.put(r_i, vList);
						templatesWithNoRequirements.get(template).put(r_a, in_inHash);
					}
				} else {
					HashMap<IRI,HashMap<IRI,Set<IRI>>> inHash = new HashMap<IRI,HashMap<IRI,Set<IRI>>>();
					HashMap<IRI,Set<IRI>> in_inHash = new HashMap<IRI,Set<IRI>>();
					in_inHash.put(r_i, vList);
					inHash.put(r_a, in_inHash);
					templatesWithNoRequirements.put(template,inHash);
				}
					
			}
			
			result.close();
		}

		LOG.info("templates with no Requirements: {}", templatesWithNoRequirements);
		return templatesWithNoRequirements;
	}
	
	
	public void autocompleteRequirements() {
		
		UpdateKB u = new UpdateKB(kb, context, ws);
		for (HashMap<String, IRI> modelForModification : this.ModelsToBeModified) {
			IRI template = modelForModification.get("template");
			IRI r_i = modelForModification.get("r_i");
			IRI r_a = modelForModification.get("r_a");
			IRI matching_template = modelForModification.get("matching_template");
			u.addRequirement(template, r_i, r_a, matching_template);
		}
	}
	
		
	public Set<IRI> selectCompatibleTemplates(IRI type) {
		Set<IRI> templates = new HashSet<>();
		String query = KB.PREFIXES +
				"select distinct ?template {\r\n" +
				"\t?template a soda:SodaliteSituation . \r\n";
			
		String defaultGraph = "\t?template rdf:type ?t .\r\n";
		String namedGraph = "\tGRAPH <"+ context + ">\r\n\t{\r\n" +
							"\t\t?template rdf:type ?t .\r\n" + 
							"\t}";
					
		query += (context != null) ? namedGraph : defaultGraph;
				
		query += "\n\t?t rdfs:subClassOf ?var\r\n" +
				"}";
			
		LOG.info("selectCompatibleTemplates: {}", query);
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("var", type));
			
		while (result.hasNext()) {
			BindingSet bindingSet = result.next();
			IRI t = (IRI) bindingSet.getBinding("template").getValue();
			templates.add(t);
		}
		result.close();
			
		return templates;
	}
	
}
