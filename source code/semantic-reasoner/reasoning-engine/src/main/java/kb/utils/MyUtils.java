package kb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import kb.repository.KB;
import kb.repository.KBConsts;

public class MyUtils {
	private MyUtils() {
		throw new IllegalStateException("MyUtils class");
	}

	private static final Logger LOG = LoggerFactory.getLogger(MyUtils.class.getName());
	
	public static String fileToString(String file) throws IOException {
		InputStream resourceAsStream = MyUtils.class.getClassLoader().getResourceAsStream(file);
		return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
	}

	public static Gson getGson(boolean pretty) {
		if (pretty)
			return new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
		return new GsonBuilder().disableHtmlEscaping().create();
	}

	public static String getStringValue(Value value) {
		if (value instanceof Literal) {
			return value.stringValue();
		} else {
			return ((IRI) value).getLocalName();
		}
	}
	
	public static String getStringPattern(String string, String spattern) {
		Pattern pattern = Pattern.compile(spattern);
		Matcher matcher = pattern.matcher(string);
		String pattern_string = null;
		if (matcher.find()) {
			pattern_string = matcher.group(1);
		}
		return pattern_string;
	}
	
	public static boolean hasPattern(String string, String spattern) {
		Pattern pattern = Pattern.compile(spattern);
		Matcher matcher = pattern.matcher(string);
		return matcher.find();
	}

	public static JsonObject getLabelIRIPair(IRI value) {
		JsonObject result = new JsonObject();

		JsonObject d = new JsonObject();
		d.addProperty("label", value.getLocalName());
		result.add(value.toString(), d);
		return result;
	}
	
	public static JsonObject getLabelIRINamespace(IRI value, IRI namespace) {

		JsonObject result = new JsonObject();

		JsonObject d = new JsonObject();
		d.addProperty("label", value.getLocalName());
		d.addProperty("namespace", namespace.toString());
		result.add(value.toString(), d);
		return result;
	}


	public static String randomString() {
		return new BigInteger(130, new SecureRandom()).toString(32);
	}
	
	//Get the value from attr attribute of json jsonString
	public static List getValueFromJson(String jsonString, String attr) throws Exception {
		Object json = new Gson().fromJson(jsonString, Object.class);
		List values = new ArrayList();
		collectAllTheKeys(values, json, attr);
		return values;
	}

	static void collectAllTheKeys(List vs, Object o, String attr) {
		Collection values = null;
		if (o instanceof Map) {
			Map map = (Map) o;
			values = map.values();
			Set keySet = map.keySet();
			for (Object object : keySet) {
				if(object.toString().equals(attr)) {
					vs.add(map.get(object).toString());
				}
			}

		} else if (o instanceof Collection) {
			values = (Collection) o;
		} else {
			return;
		}

		for (Object value : values) {
			collectAllTheKeys(vs, value, attr);
		}
	}
	
	//Returns if two json objects have same values
	public static boolean equals(String s1, String s2) {
		Gson g = new Gson();
		Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
		Map<String, Object> firstMap = g.fromJson(s1, mapType);
		Map<String, Object> secondMap = g.fromJson(s2, mapType);
		MapDifference<String, Object> diff =  Maps.difference(firstMap, secondMap);
		return diff.areEqual();
    }
	
	public static Set<IRI> getResourceIRIs(KB kb, IRI namespace, Set<String> resourceNames) {
		Set<IRI> resourceIRIs = new HashSet<>();
		for (String r: resourceNames) {
			resourceIRIs.add(kb.factory.createIRI(namespace + r));
		}
		
		return resourceIRIs;
	}
	
	//Extract the namespace from a sodalite iri
	public static String getNamespaceFromIRI(String iri) {
		return getStringPattern(iri, "(.*\\/)[a-zA-Z0-9_]+");
		
	}
	
	/*e.g. input: https://www.sodalite.eu/ontologies/workspace/1/vehicleiotrefac/
	 *     output: vehicleiotrefac */
	public static String getNamespaceFromFullIRI(String iri) {
		return getStringPattern(iri, ".*\\/([a-zA-Z0-9_]+)");
	}
	
	/*namespace: docker
	 * https://www.sodalite.eu/ontologies/workspace/1/docker/ is returned*/
	public static String getFullNamespaceIRI(KB kb, String namespace) {
		/*There are models that have references to local types with dedicated namespace. e.g. derived_from hpc/node1.
		 hpc namespace is not present in KB yet, so there is no reason to check if it exists in KB*/
		/*final String[] uri = {null};
		
		List<Resource> list = Iterations.asList(kb.connection.getContextIDs());
		Map<String,String> namespaces = list.stream().
												collect(Collectors.toMap(
												x -> x.toString(),
												x -> getNamespaceFromFullIRI(x.toString())
												));
		
		namespaces.forEach((key, val) -> {
			if (namespace.equals(val)) {
				uri[0] = key;
			}
		});
		
		return uri[0];*/
		return KB.BASE_NAMESPACE + namespace + KBConsts.SLASH;
	}
	
	/*
	 *  namespace/resourcename
	 * e.g. docker/sodalite.nodes.DockerHost, docker is returned
	 */
	public static String getNamespaceFromReference(String resource) {
		String[] split = resource.split("\\/");
		if (split.length > 1)
			return split[0];
		return null;
	}
	
	/*
	 *  namespace/resourcename= e.g. docker/sodalite.nodes.DockerHost,
	 *  sodalite.nodes.DockerHost is returned
	 */
	public static String getReferenceFromNamespace(String resource) {
		String[] split = resource.split("\\/");
		if (split.length > 1) {
			String[] res =  split[1].split("\\@");
			if (res.length > 1) 
				return res[0];
			return split[1];
		}
		return null;
	}
	
	/* A) resource: docker/sodalite.nodes.Dockerhost
	 * https://www.sodalite.eu/ontologies/workspace/1/docker/sodalite.nodes.Dockerhost is returned
	 * B) case versioned resource: snow/snow-vm@v1, 
	 * https://www.sodalite.eu/ontologies/workspace/1/snow/v1/snow-vm is returned
	 * The versioned resources are only for AADM resources
	*/
	public static String getFullResourceIRI(String resource, KB kb) {
		String resourceIRI;
		
		System.err.println("resource = " + resource);	
		String namespace = getNamespaceFromReference(resource);
		String name = getReferenceFromNamespace(resource);
		System.err.println("getFullResourceIRI namespace = " + namespace + ", name = " + name);	
		if (namespace != null) {
			String version = getVersionFromNamedResource(resource);
			System.err.println("version = " + version);
			resourceIRI = (version == null) ?  getFullNamespaceIRI(kb, namespace) + name : getFullNamespaceIRI(kb, namespace) + version + KBConsts.SLASH + name;
		} else {
			if (hasPattern(resource, "^tosca."))
				resourceIRI = KB.TOSCA + resource;
			else {
				String version = getVersionFromGlobalResource(resource);
				resourceIRI = (version == null) ? KB.GLOBAL + resource : KB.GLOBAL + KBConsts.SLASH + resource;
			}
		}
		
		LOG.info("getFullResourceIRI resourceIRI:" + resourceIRI);
		
		return resourceIRI;
	}
	
	public static boolean validNamespace(KB kb, String namespace) {
		List<Resource> list = Iterations.asList(kb.connection.getContextIDs());
		
		if (list.contains(kb.factory.createIRI(namespace))) 
			return true;
		return false;
	}

	/* context: https://www.sodalite.eu/ontologies/workspace/1/snow/
	 * snow is returned
	 */
	public static String getNamespaceFromContext(String context) {
		return MyUtils.getStringPattern(context, "^"+ KB.BASE_NAMESPACE + "(.*)/");
	}
	
	public static boolean isValidURL(String url) {
		/* Try creating a valid URL */
		try {
			new URL(url).toURI();
			return true;
		}// If there was an Exception
		// while creating URL object
		catch (Exception e) {
			return false;
		}
	}
	
	/*
	 * aadmUri: https://www.sodalite.eu/ontologies/workspace/1/dl1tesatvj9473bef35afqgg2i/AADM_tp0qm60qvengrg5aafg8d83gq/version1
	 * https://www.sodalite.eu/ontologies/workspace/1/dl1tesatvj9473bef35afqgg2i/AADM_tp0qm60qvengrg5aafg8d83gq is returned
	 */
	public static String getAADMUriWithoutVersion(IRI aadmUri) {
		return getStringPattern(aadmUri.stringValue(), "(.*\\/AADM_[a-zA-Z0-9]+)");
	}
	
	/* resource = snow/snow-vm@v1
	 * @ is the separator for the version
	 * v1 is returned
	 */
	public static String getVersionFromNamedResource(String resource) {
		return getStringPattern(resource,".*\\/[a-zA-Z0-9\\.\\-\\_]+@([a-zA-Z0-9]+)");
	}
	
	/* Get version from a resource without namespace
	 * resource = snow-vm@v1
	 * @ is the separator for the version
	 * v1 is returned
	 */
	public static String getVersionFromGlobalResource(String resource) {
		return getStringPattern(resource,".*[a-zA-Z0-9.-_]+@([a-zA-Z0-9]+)");
	}
}
