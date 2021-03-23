package httpclient;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import httpclient.dto.AuthErrorModel;
import httpclient.exceptions.AuthException;
import kb.configs.ConfigsLoader;
import kb.utils.MyUtils;

public class AuthUtil {
	private static final Logger LOG = LoggerFactory.getLogger(AuthUtil.class.getName());
	
	static ConfigsLoader configInstance = ConfigsLoader.getInstance();
	static {
		configInstance.loadProperties();
	}
	
	private AuthUtil() {
		throw new IllegalStateException("AuthUtil class");
	}
	
	public static boolean authentication() {
		return configInstance.getKeycloak() != null;
	}
	
	/**
	 * Checks if the rolesInput are contained in roles
	 * @param rolesInput The required roles
	 * @param roles The assigned roles
	 */
	public static void checkRoles(List<String> rolesInput , List<String> roles) throws AuthException {
		List<String> notAssignedRoles = new ArrayList<>();
		for (String role : rolesInput) {
			if (!roles.contains(role)) {
				notAssignedRoles.add(role);
			}
		}
	
		if (!notAssignedRoles.isEmpty()) {
			List<AuthErrorModel> errors = new ArrayList<>();
			errors.add(new AuthErrorModel(LocalDateTime.now(), notAssignedRoles.toString() + " role(s) not assigned", HttpStatus.FORBIDDEN, 403));
			throw new AuthException(errors);
		}
		
	}
	
	/**
	 * Some services have imports as parameter. Those imports which are the namespaces
	 * are converted to roles
	 * @param namespaces It contains the imports e.g. docker, snow
	 * @param typeOfRole e.g. _aadm_w
	 * @return The created roles
	 */
	public static List<String> createRolesFromNamespaces(List<String> namespaces, String typeOfRole){
		List<String> roles = new ArrayList<>();
		for (String role: namespaces) {
			if (!role.equals("null"))
				roles.add(role + typeOfRole);
		}
		LOG.info( "createRolesFromNamespaces = {}",  roles);
		return roles;
	}
	
	/**
	 * Some services have not imports but only one namespace
	 * @param namespace It contains the namespace
	 * @param typeOfRole e.g. _aadm_w
	 * @return The created role
	 */
	public static List<String> createRoleFromNamespace(String namespace, String typeOfRole) {
		List<String> roles = new ArrayList<>();
		String role;
		if(typeOfRole.equals(AuthConsts.AADM_W) || typeOfRole.equals(AuthConsts.RM_W))
			role = namespace.isEmpty() ? AuthConsts.GLOBAL + typeOfRole : namespace + typeOfRole;
		else 
			role = namespace + typeOfRole;
		
		roles.add(role);
		LOG.info( "createRoleFromNamespace = {}",  roles);
		return roles;
	}
	
	/**
	 * Create roles from resources by retrieving the namespaces of the resources
	 * @param resources e.g. docker/dockerNode
	 * @param typeOfRole e.g. _rm_r
	 * @return the roles 
	 */
	public static List<String> createRolesFromResources(List<String> resources, String typeOfRole) {
		List<String> roles = new ArrayList<>();
		for (String resource: resources) {
			String namespace = MyUtils.getNamespaceFromReference(resource);
			if(namespace != null)
				roles.add(namespace + typeOfRole);
		}
		LOG.info( "createRolesFromResources = {}",  roles);
		return roles;
	}
	
	public static String getTokenFromBearerHeader(List<String> authHeaders) {
		LOG.info("authHeaders", authHeaders.get(0));
		String[] authparts = authHeaders.get(0).split(" ");
		String token = null;
		
		if (!authparts[0].equals("Bearer"))
			LOG.error("not bearer: {}", authparts[0]);
		else
			token = authparts[1];
		LOG.info("token: {}", token);
		return token;
	}
	
}
