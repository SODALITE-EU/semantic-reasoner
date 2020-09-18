package kb.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.net.HttpURLConnection;


public class MyFileUtil {
	
	public static String ANSIBLE_PATH;
	public static String REASONER_SERVER;
	public static String ANSIBLE_FOLDER = "Ansibles/";
	
	
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
			System.out.println("path = " + path);
			File myObj = new File(path);
			if (myObj.createNewFile()) {
				System.out.println("File created: " + myObj.getName());
			} else {
				System.out.println("File already exists.");
			}

			myWriter = new FileWriter(path);
			//removing the outer double quotes
			String _content = content.replaceAll("^\"|\"$", "");
			myWriter.write(_content);
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
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
