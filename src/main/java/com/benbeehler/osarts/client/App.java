package com.benbeehler.osarts.client;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.benbeehler.osarts.client.io.FIO;

public class App {
	
	private static final File configf = new File("./config.json");
	private static JSONObject config;
	private static String dir = "/";
	private static String addr = "localhost";
	private static int port = 80;
	
	private static List<File> ignore =
			new ArrayList<>();
	
	private static List<File> files = 
			new ArrayList<>();
	
	public static void main(String[] args) {
		if(!configf.exists()) {
			try {
				configf.createNewFile();
				
				config = new JSONObject("{}");
				config.put("system.sendDirectory", "./");
				config.put("system.address", addr);
				config.put("system.port", port);
				
				FIO.write(configf, config.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		dir = dir + "/";
		
		try {
			config = new JSONObject(FIO.readF(configf));
			
			dir = config.getString("system.sendDirectory");
			addr = config.getString("system.address");
			port = config.getInt("system.port");
			System.out.println("Resources will be distributed from " + dir);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		File ig = new File(dir + "/" + "ignore");
		
		List<String> fnames;
		try {
			fnames = FIO.readFToList(ig);
			for(String name : fnames) {
				ignore.add(new File(dir + "/" + name));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		files.addAll(listf(new File(dir)));
		
		files = files.stream().distinct().collect(Collectors.toList());
		
		for(File file : files) {
			try {
				System.out.println("Sending " + file.getName());
				sendFile(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static List<File> listf(File directory) {
		List<File> files = new ArrayList<>();
		
	    File[] fList = directory.listFiles();
	    if(fList != null) {
	        for (File file : fList) {
	            if (file.isFile()) {
	            	ignore.forEach(f -> {
	            		if(!f.getAbsolutePath().equals(file.getAbsolutePath())) {
	            			System.out.println("Added " + file.getAbsolutePath());
		            		files.add(file);
	            		} else {
		    				System.out.println("Ignoring " + file.getName());
		            	}
	            	});
	            } else if (file.isDirectory()) {
	            	ignore.forEach(f -> {
	            		if(!f.getAbsolutePath().equals(file.getAbsolutePath())) {
	    	                files.addAll(listf(file));
	            		} else {
		    				System.out.println("Ignoring " + file.getName());
		            	}
	            	});
	            }
	        }
	    }
	    
	    return files;
	}
	
	private static void sendFile(File file) throws IOException {
		String p = file.getAbsolutePath().replace("\\", "/");
		String fl = p.replace(dir, "").trim();
		sendData("SIGNATURE_NEWFILE " + fl);
		
		List<String> list = splitEqually(FIO.readF(file), 10000);
		
		int i = 0;
		
		for(String str : list) {
			i++;
			
			try {
				System.out.println("Sending " + file.getName() + " (" + ((float) i/list.size()) + "%)");
				sendData("SIGNATURE_APPEND " + str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//sendData("SIGNATURE_APPEND " + FIO.readF(file));
	
		sendData("SIGNATURE_ENDFILE");
	}
	
	private static void sendData(String data) throws IOException {
		Socket socket = new Socket(addr, port);
		DataOutputStream i = new DataOutputStream(socket.getOutputStream());
		i.writeUTF(data);
		i.flush();
		socket.close();
	}
	
	public static List<String> splitEqually(String text, int size) {
	    List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);

	    for (int start = 0; start < text.length(); start += size) {
	        ret.add(text.substring(start, Math.min(text.length(), start + size)));
	    }
	    return ret;
	}
}
