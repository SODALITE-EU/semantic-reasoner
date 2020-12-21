package kb.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import kb.configs.ConfigsLoader;


public class MyFileUtil {
	private static final Logger LOG = Logger.getLogger(MyFileUtil.class.getName());
	
	public final static String ANSIBLE_PATH;
	public final static String REASONER_SERVER;
	public final static String ANSIBLE_FOLDER = "Ansibles/";
	
	
	static {
		ConfigsLoader c  = ConfigsLoader.getInstance();
		ANSIBLE_PATH = c.getAnsiblePath();
		REASONER_SERVER = c.getReasonerServer();
	}
	
	public static String uploadFile(String content) throws IOException {
		
		String fileName = null;
		FileWriter myWriter = null;
		try {
			//Files.write(Paths.get("C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0_Tomcat92\\webapps\\Ansibles\\" + fileName), content.getBytes());
			//String path = "C:\\Program Files\\Apache Software Foundation\\Tomcat 9.0_Tomcat92\\webapps\\Ansibles\\" + fileName;
        	
			fileName = UUID.randomUUID().toString();
			String path = ANSIBLE_PATH + fileName;
			LOG.log(Level.INFO, "path = {0}", path);
			File myObj = new File(path);
			if (myObj.createNewFile()) {
				LOG.log(Level.INFO, "File created: {0}", myObj.getName());
			} else {
				LOG.log(Level.WARNING, "File already exists.");
			}

			myWriter = new FileWriter(path);
			//removing the outer double quotes
			String _content = content.replaceAll("^\"|\"$", "");
			myWriter.write(_content);
			myWriter.close();
			LOG.log(Level.INFO, "Successfully wrote to the file.");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (myWriter != null)
				myWriter.close();
		}

		String playbookURL = REASONER_SERVER + ANSIBLE_FOLDER + fileName;
		return playbookURL;
	}

}
