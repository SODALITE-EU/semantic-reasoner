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

import kb.repository.KB;
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
	String aadmId;

	String ws;
	IRI context;
	
	List<RequirementExistenceModel> optionalModels = new ArrayList<RequirementExistenceModel>();
	List<RequirementExistenceModel> requiredModels = new ArrayList<RequirementExistenceModel>();
	List<RequirementExistenceModel> modifiedModels = new ArrayList<RequirementExistenceModel>();
		
	public RequirementExistenceValidation(String aadm, boolean complete, KB kb, String ws, IRI context) {
		super(kb);
		this.aadmId = aadm;
		this.complete = complete;
		this.kb = kb;
		this.ws = ws;
		this.context = context;
	}
	
	public List<RequirementExistenceModel> getModifiedModels() {
		return this.modifiedModels;
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
					 if (templates.size() == 1) {
						 if (complete) {
							 UpdateKB u = new UpdateKB(kb, context, ws);
							 u.addRequirement(template, r_i, r_a, templates.iterator().next());
							 modifiedModels.add(new RequirementExistenceModel(template, type, r_a, r_i, templates, required));
						 } else 
							 models.add(new RequirementExistenceModel(template, type, r_a, r_i, templates));
					 }	else if (templates.isEmpty()){
						 if (required)
							 models.add(new RequirementExistenceModel(template, type, r_a, r_i, templates, "No matching target template found"));
					 } else {
						 // more than one template found
					 	models.add(new RequirementExistenceModel(template, type, r_a, r_i, templates, templates.size() + " matching target templates found"));
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
		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("aadmId", kb.getFactory().createLiteral(aadmId)));

			//HashMap<Template, HashMap<r_a, HashMap<r_inner, node types>>>
			//e.g. HashMap<snow-vm,HashMap<protected_by, HashMap<node, sodalite.nodes.OpenStack.SecurityRules>
			HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>> templatesWithNoRequirements = new HashMap<IRI, HashMap<IRI,HashMap<IRI,Set<IRI>>>>();

			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				IRI template = (IRI) bindingSet.getBinding("template").getValue();
				IRI v = (IRI) bindingSet.getBinding("v").getValue();
				IRI r_a = (IRI) bindingSet.getBinding("r_a").getValue();
				IRI r_i = (IRI) bindingSet.getBinding("r_i").getValue();
				
				Set <IRI> vList= new HashSet<IRI>();	
				vList.add(v);
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

			return templatesWithNoRequirements;
		}
		
		public Set<IRI> selectCompatibleTemplates(IRI type) {
			Set<IRI> templates = new HashSet<>();
			String query = KB.PREFIXES +
					"select distinct ?template {\r\n" +
					"?template a soda:SodaliteSituation ; \r\n" +
					" rdf:type ?t .\r\n" + 
					" ?t rdfs:subClassOf ?var" +
					"}";
			
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
