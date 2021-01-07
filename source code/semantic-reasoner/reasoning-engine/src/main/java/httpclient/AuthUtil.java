package httpclient;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.springframework.http.HttpStatus;

import httpclient.dto.AuthErrorModel;
import httpclient.exceptions.AuthException;
import kb.configs.ConfigsLoader;

public class AuthUtil {	
	private AuthUtil() {
		throw new IllegalStateException("AuthUtil class");
	}
	
	static ConfigsLoader configInstance = ConfigsLoader.getInstance();
	static {
		configInstance.loadProperties();
	}
	
	public static boolean authentication() {
		return ConfigsLoader.AUTHENVS.contains(configInstance.getEnvironment());
	}
	
	public static void checkRole(String role, ArrayList<String> roles) throws AuthException {
		if (!roles.contains(role)) {
			List<AuthErrorModel> errors = new ArrayList<AuthErrorModel>();
			errors.add(new AuthErrorModel(LocalDateTime.now(), role + " role not assigned", HttpStatus.FORBIDDEN));
			throw new AuthException(errors);
		}
	}
	
	public static void main(String[] args) {

	}

}
