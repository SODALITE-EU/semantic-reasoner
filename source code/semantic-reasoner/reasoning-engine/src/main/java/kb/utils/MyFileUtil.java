package kb.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kb.configs.ConfigsLoader;


public class MyFileUtil {
	private static final Logger LOG = LoggerFactory.getLogger(MyFileUtil.class.getName());
	
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
			LOG.info("path = {}", path);
			File myObj = new File(path);
			if (myObj.createNewFile()) {
				LOG.info("File created: {}", myObj.getName());
			} else {
				LOG.warn("File already exists.");
			}

			myWriter = new FileWriter(path);
			//removing the outer double quotes
			String _content = content.replaceAll("(?:^\")|(?:\"$)", "");
			myWriter.write(_content);
			myWriter.close();
			LOG.info( "Successfully wrote to the file.");
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} finally {
			if (myWriter != null)
				myWriter.close();
		}

		String playbookURL = REASONER_SERVER + ANSIBLE_FOLDER + fileName;
		return playbookURL;
	}

}
