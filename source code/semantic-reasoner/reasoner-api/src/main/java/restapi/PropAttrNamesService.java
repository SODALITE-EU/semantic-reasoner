package restapi;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import kb.KBApi;

/** A service that returns the names of the properties or attributes of a template
 * @author George Meditskos
 * @author Zoe Vasileiou
 * @version 1.0
 * @since 1.0
*/
@Path("/prop-attr-names")
@Api()
public class PropAttrNamesService extends AbstractService {
	private static final Logger LOG = LoggerFactory.getLogger(PropAttrNamesService.class.getName());

	/**
	 * Getting the names of properties or attributes of a template.
	 * @param resource The resource name
	 * @param element The element type
	 * @throws IOException If your input format is invalid
	 * @return The requirements in JSON format
	*/
	@GET
	@Produces("application/json")
	@ApiOperation(
			value = "Returns the names of the properties of a single TOSCA resource",
//			response = String.class,
			responseContainer = "List")
	public Response getProperty(
			@ApiParam(
					value = "A TOSCA resource, e.g. a node",
					required = true,
					defaultValue = "my.nodes.SkylineExtractor") @QueryParam("resource") String resource,
			@ApiParam(
					value = "prop or attr",
					required = true,
					defaultValue = "prop") @QueryParam("element") String element)
		throws IOException {
		
		LOG.info("resource={}, element={}", resource, element);
		
		KBApi api = new KBApi();
		
		Set<String> names = new HashSet<>();
		
		if (element != null && (element.equals("prop") || element.equals("attr")))
			names = api.getPropAttrNames(api.getResourceIRI(resource), element);
		
		api.shutDown();
		JsonObject _data = new JsonObject();
		JsonArray array = new JsonArray();
		for (String n : names) {
			array.add(n);
		}
		_data.add("data", array);

		return Response.ok(_data.toString()).build();
	}
}
