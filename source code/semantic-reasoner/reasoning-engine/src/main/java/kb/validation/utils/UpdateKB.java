package kb.validation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import kb.repository.KB;
import kb.utils.MyUtils;

public class UpdateKB {
	private static final Logger LOG = LoggerFactory.getLogger(UpdateKB.class.getName());

	KB kb;
	IRI context;
	String ws;
	
	RepositoryConnection conn;
	ValueFactory f;
	
	public UpdateKB(KB kb, IRI context, String ws) {
		this.kb = kb;
		this.context = context;
		this.ws = ws;
		
		this.conn = kb.getConnection();
		this.f = kb.getFactory();
	}
	
	
	public void addRequirement (IRI template, IRI r_i, IRI r_a, IRI matching_template) {
		LOG.info("Adding requirement to template= {}\n\t {} \n\t\t {} \n\t\t\t {}", new Object[] {
								template, r_a, r_i, matching_template
							});
		LOG.info("context= {}", context);
		RepositoryResult<Statement> statements = conn.getStatements(template, f.createIRI(KB.SODA + "hasContext"), null, context, null);
		IRI desc = (IRI) statements.next().getObject();
		 
		IRI reqClassifierKB = f.createIRI(ws + "ReqClassifer_" + MyUtils.randomString());
		conn.add(desc, f.createIRI(KB.TOSCA + "requirements"), reqClassifierKB);
		conn.add(reqClassifierKB, f.createIRI(KB.DUL + "classifies"), r_a);
		IRI parameterClassifierKB = f.createIRI(ws + "ParamClassifier_" + MyUtils.randomString());
		conn.add(reqClassifierKB, f.createIRI(KB.DUL + "hasParameter"), parameterClassifierKB);
		conn.add(parameterClassifierKB, f.createIRI(KB.DUL + "classifies"), r_i);
		conn.add(parameterClassifierKB, f.createIRI(KB.TOSCA + "hasObjectValue"), matching_template);
	}
	
}
