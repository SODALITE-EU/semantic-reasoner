package kb.validation;

import org.eclipse.rdf4j.model.Model;

import kb.repository.KB;

public class ValidationManager {

	protected KB kb; // in order to query the schema - tier1

	protected Model exchangeModel; // the submitted data (tier2)

	public ValidationManager() {
		kb = new KB();
	}
	
	public ValidationManager(KB kb, Model model) {
		this.kb = kb;
		this.exchangeModel = model;
	}

	public ValidationManager(Model model) {
		kb = new KB();
		this.exchangeModel = model;
	}

	public void shutDown() {
		kb.shutDown();
	}

//	public IRI getRequirementNode(IRI ctx, IRI r_a) {
//		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), KBApi.PREFIXES +
//				"select * {"
//				+ "?ctx tosca:requirements "
//				+ "	[DUL:classifies ?r_a; DUL:hasParameter [DUL:classifies tosca:node; DUL:hasRegion ?r_d_node]] .} ",
//				new SimpleBinding[] { new SimpleBinding("ctx", ctx), new SimpleBinding("r_a", r_a) });
//		IRI r_d_node = null;
//		while (result.hasNext()) {
//			BindingSet bindingSet = result.next();
//			r_d_node = (IRI) bindingSet.getBinding("r_d_node").getValue();
//			break;
//		}
//		result.close();
//		return r_d_node;
//	}
//
//	public IRI getTemplateRequirementValueType(IRI template, IRI r_a) {
//		TupleQueryResult result = QueryUtil.evaluateSelectQuery(kb.getConnection(), KBApi.PREFIXES +
//				"select * {"
//				+ "?template soda:hasContext ?ctx. "
//				+ "?ctx tosca:requirements [DUL:classifies ?r_a; DUL:hasRegion ?r_a_node] ."
//				+ "?r_a_node sesame:directType ?type_r_a_node . "
//				+ "?type_r_a_node rdfs:subClassOf tosca:tosca.entity.Root ."
//				+ "} ",
//				new SimpleBinding[] { new SimpleBinding("template", template), new SimpleBinding("r_a", r_a) });
//		IRI type_r_a_node = null;
//		while (result.hasNext()) {
//			BindingSet bindingSet = result.next();
//			type_r_a_node = (IRI) bindingSet.getBinding("type_r_a_node").getValue();
//			break;
//		}
//		result.close();
//		return type_r_a_node;
//	}

}
