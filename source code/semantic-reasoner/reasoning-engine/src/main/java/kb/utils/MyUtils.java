package kb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

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

}
