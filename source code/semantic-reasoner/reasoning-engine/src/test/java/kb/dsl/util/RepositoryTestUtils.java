package kb.dsl.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class RepositoryTestUtils {
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryTestUtils.class.getName());
	
	private RepositoryTestUtils() {
		throw new IllegalStateException("RepositoryTestUtils class");
	}

	public static void loadCoreOntologies(RepositoryConnection repositoryConnection) {
		LOG.info("loadCoreOntologies");
		try {
			InputStream input1 =
					RepositoryTestUtils.class.getResourceAsStream("/import/DUL.rdf");
			repositoryConnection.add(input1, "", RDFFormat.RDFXML);

			InputStream input2 =
					RepositoryTestUtils.class.getResourceAsStream("/core/sodalite-metamodel.ttl");
			repositoryConnection.add(input2, "", RDFFormat.TURTLE);

			InputStream input3 =
					RepositoryTestUtils.class.getResourceAsStream("/core/tosca-builtins.ttl");
			repositoryConnection.add(input3, "", RDFFormat.TURTLE);
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public static String fileToString(String file) throws IOException {
		InputStream resourceAsStream = RepositoryTestUtils.class.getClassLoader().getResourceAsStream(file);
		return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
	}
}
