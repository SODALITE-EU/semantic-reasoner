package kb.dsl.artifact.files;

import java.io.IOException;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.repository.KB;
import kb.repository.KBConsts;
import kb.utils.MyFileUtil;
import kb.utils.MyUtils;

public class HandleArtifactFile {
	
	private static final Logger LOG = LoggerFactory.getLogger(HandleArtifactFile.class.getName());
	KB kb;
	IRI namespace;
	
	public HandleArtifactFile(KB kb, IRI namespace) {
		this.kb = kb;
		this.namespace = namespace;
	}
	
	public IRI linkArtifactURLtoTheOntology (IRI contentIRI, Model model, ModelBuilder nodeBuilder) {
		IRI paramClassifierKB = null;
		Literal value = Models
				.objectLiteral(model.filter(contentIRI, kb.factory.createIRI(KB.EXCHANGE + "value"), null))
				.orElse(null);
		
		LOG.info("value: {}", value);
		
		if (value != null) {
			String fileUrl = null;
			try {
				fileUrl = MyFileUtil.uploadFile(value.toString());
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
			
			Value fileUrlValue = null;
			if (fileUrl != null)
				fileUrlValue = kb.getFactory().createLiteral(fileUrl);
				
			//This parameter is not added to the KB model, it is only added to aadm json
			//e.g. url: "http://160.40.52.200:8084/Ansibles/b035b421-3aba-4cfb-b856-dfc473e5c71d"
			//String ws = MyUtils.getNamespaceFromIRI(classifier.toString());
			
			
			paramClassifierKB = kb.factory.createIRI(namespace + KBConsts.PARAM_CLASSIFIER + MyUtils.randomString());
			nodeBuilder.add(paramClassifierKB, RDF.TYPE, "soda:SodaliteParameter");
			nodeBuilder.add(paramClassifierKB, kb.factory.createIRI(KB.DUL + "classifies", namespace + "url"));
		}
		
		return paramClassifierKB;
	}

}
