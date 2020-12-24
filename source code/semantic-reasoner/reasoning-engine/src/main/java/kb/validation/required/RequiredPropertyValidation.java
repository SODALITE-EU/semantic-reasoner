package kb.validation.required;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.impl.SimpleBinding;

import kb.repository.KB;
import kb.utils.QueryUtil;
import kb.validation.exceptions.models.RequiredPropertyAttributeModel;

public class RequiredPropertyValidation {
	private static final Logger LOG = Logger.getLogger(RequiredPropertyValidation.class.getName());

	Literal templateType;
	String templateName;
	Set<String> exchangeProperties;
	KB kb;
	Model aadm;

	List<RequiredPropertyAttributeModel> models = new ArrayList<RequiredPropertyAttributeModel>();

	public RequiredPropertyValidation(String templateName, Literal templateType, Set<String> exchangeProperties,
			KB kb) {
		this.templateType = templateType;
		this.templateName = templateName;
		this.exchangeProperties = exchangeProperties;
		this.kb = kb;
	}

	public List<RequiredPropertyAttributeModel> validate() {

		// for each node template, find the node type and get the required properties
		String query = KB.PREFIXES + optionalPropertiesQuery;
		List<String> schemaProperties = Iterations
				.asSet(QueryUtil.evaluateSelectQuery(kb.getConnection(), query, new SimpleBinding("var", templateType)))
				.stream()
				.map(x -> ((IRI) x.getBinding("property").getValue()).getLocalName()).collect(Collectors.toList());

		LOG.log(Level.INFO, "validation: [{0}]  {1}", new Object[] {templateType, schemaProperties});
		LOG.log(Level.INFO, "exchange properties: {0}", exchangeProperties);

		boolean found = false;
		for (String sch : schemaProperties) {
			for (String ex : exchangeProperties) {
				if (sch.equals(ex)) {
					found = true;
					break;
				}
			}
			if (!found) {
				models.add(new RequiredPropertyAttributeModel(templateName, templateType.stringValue(), sch, null));
			}
			found = false;
		}

		return models;

	}

	private String optionalPropertiesQuery = "select distinct ?property\r\n" +
			"where {\r\n" +
			"	?resource soda:hasInferredContext ?context .\r\n" +
			"	FILTER (strends(str(?resource), ?var)).\r\n" +
			"	?context tosca:properties ?concept .\r\n" +
			"	?concept DUL:classifies ?property .\r\n" +
			"	{\r\n" +
			"        ?concept DUL:hasParameter [DUL:classifies tosca:required; tosca:hasDataValue true].\r\n" +
			"    }\r\n" +
			"    UNION\r\n" +
			"    {\r\n" +
			"        FILTER NOT EXISTS {?concept DUL:hasParameter [DUL:classifies tosca:required; tosca:hasDataValue []]}.\r\n"
			+
			"    }\r\n" +
			"	FILTER NOT EXISTS {\r\n" +
			"			?resource soda:hasInferredContext ?context2 .\r\n" +
			"			FILTER(?context != ?context2).\r\n" +
			"			?context2 tosca:properties ?classifier2.\r\n" +
			"			?classifier2 DUL:classifies ?property .\r\n" +
			"			\r\n" +
			"			?resource2 soda:hasContext ?context2 .\r\n" +
			"			#FILTER(?resource != ?resource2).	\r\n" +
			"			FILTER(?resource2 != owl:Nothing).\r\n" +
			"			?resource2 rdfs:subClassOf ?resource.	\r\n" +
			"	}\r\n" +
			"}\r\n" +
			"";

}
