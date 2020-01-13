package kb.repository;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RemoteRepositoryManager;

public class SodaliteRepository {

	private RemoteRepositoryManager _manager;

	public SodaliteRepository(String serverURL, String username, String password) {

		try {
			_manager = new RemoteRepositoryManager(serverURL);
			_manager.setUsernameAndPassword(username, password);
			_manager.init();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Repository getRepository(String id) throws RepositoryConfigException, RepositoryException {
		return _manager.getRepository(id);
	}

	public void shutDown(String CONTEXT) {
		System.out.println("closing GraphDb manager [" + CONTEXT + "]");
		if (_manager != null) {
			try {
				_manager.shutDown();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public RemoteRepositoryManager getManager() {
		return _manager;
	}

}
