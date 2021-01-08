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
		return ConfigsLoader.AUTHENVS.contains(configInstance.getEnvironment());
	}
	
	/**
	 * Checks if the roles_input are contained in roles
	 * @param roles_input The required roles
	 * @param roles The assigned roles
	 */
	public static void checkRoles(ArrayList<String> roles_input , ArrayList<String> roles) throws AuthException {
		ArrayList<String> not_assigned_roles = new ArrayList<String>();
		for (String role : roles_input) {
			if (!roles.contains(role)) {
				not_assigned_roles.add(role);
			}
		}
	
		if (!not_assigned_roles.isEmpty()) {
			List<AuthErrorModel> errors = new ArrayList<AuthErrorModel>();
			errors.add(new AuthErrorModel(LocalDateTime.now(), not_assigned_roles.toString() + " role(s) not assigned", HttpStatus.FORBIDDEN));
			throw new AuthException(errors);
		}
		
	}
	
	/**
	 * Some services have imports as parameter. Those imports which are the namespaces
	 * are converted to roles
	 * @param namespaces It contains the imports
	 * @param typeOfRole e.g. _aadm_w
	 */
	public static ArrayList<String> createRolesFromNamespaces(List<String> namespaces, String typeOfRole){
		ArrayList<String> roles = new ArrayList<String>();
		for (String role: namespaces) {
			roles.add(role + typeOfRole);
		}
		roles.add(AuthConsts.GLOBAL + typeOfRole);
		LOG.info( "createRolesFromNamespaces = {}",  roles);
		return roles;
	}
	
	/**
	 * Some services have not imports but only one namespace
	 * @param namespace It contains the namespace
	 * @param typeOfRole e.g. _aadm_w
	 */
	public static ArrayList<String> createRoleFromNamespace(String namespace, String typeOfRole) {
		ArrayList<String> roles = new ArrayList<String>();
		String role = namespace.isEmpty() ? AuthConsts.GLOBAL + typeOfRole : namespace + typeOfRole;
		roles.add(role);
		LOG.info( "createRoleFromNamespace = {}",  roles);
		return roles;
	}
}
