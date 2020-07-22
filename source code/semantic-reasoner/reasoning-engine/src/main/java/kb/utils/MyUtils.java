package kb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class MyUtils {

	public static String fileToString(String file) throws IOException {
		InputStream resourceAsStream = MyUtils.class.getClassLoader().getResourceAsStream(file);
		return IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8.name());
//		return FileUtils.readFileToString(new File(), StandardCharsets.UTF_8);
	}

	public static Gson getGson(boolean pretty) {
		if (pretty)
			return new GsonBuilder().setPrettyPrinting().create();
		return new GsonBuilder().create();
	}

	public static String getStringValue(Value value) {
		if (value instanceof Literal) {
			return value.stringValue();
		} else {
			return ((IRI) value).getLocalName();
			// return ((IRI) value).toString();
		}
	}
	
	public static String getStringPattern (String string, String spattern) {
		Pattern pattern = Pattern.compile(spattern);
		Matcher matcher = pattern.matcher(string);
		String pattern_string = null;
		if (matcher.find()) {
			pattern_string = matcher.group(1);
		}
		return pattern_string;
	}

	public static JsonObject getLabelIRIPair(IRI value) {
		JsonObject result = new JsonObject();

		JsonObject d = new JsonObject();
		d.addProperty("label", value.getLocalName());
		result.add(value.toString(), d);
		return result;
	}

	public static String randomString() {
		return new BigInteger(130, new SecureRandom()).toString(32);
	}
	
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
	
	
	public static boolean equals(String s1, String s2) {
		Gson g = new Gson();
		Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
		Map<String, Object> firstMap = g.fromJson(s1, mapType);
		Map<String, Object> secondMap = g.fromJson(s2, mapType);
		MapDifference<String, Object> diff =  Maps.difference(firstMap, secondMap);
		return diff.areEqual();
    }

}
