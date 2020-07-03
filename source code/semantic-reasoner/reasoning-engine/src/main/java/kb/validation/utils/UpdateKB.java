package kb.validation.utils;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import kb.repository.KB;
import kb.utils.MyUtils;

public class UpdateKB {

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
		System.out.println("Adding requirement to template="+ template + "\n\t"+ r_a + "\n\t\t" + r_i + "\n\t\t\t" + matching_template);
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
