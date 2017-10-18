package com.hetty.plugin;

import java.io.File;
import java.util.List;

import com.hetty.conf.HettyConfig;
import com.hetty.conf.XmlConfigParser;
import com.hetty.core.HettySecurity;
import com.hetty.core.ServiceHandler;
import com.hetty.object.Application;
import com.hetty.object.Service;
import com.hetty.object.ServiceVersion;

public class XmlConfigPlugin implements IPlugin{

	@Override
	public boolean start() {
		String configFile = HettyConfig.getInstance().getpropertiesFile();
		String[] fileArr = configFile.split(",");
		
		for(String file : fileArr){
			String filePath = System.getProperty("user.dir") + File.separator
					+ "config" + File.separator + file;
			File f = new File(filePath);
			if (!f.exists()) {
				continue;
			}
			XmlConfigParser configParser = new XmlConfigParser(filePath);
			
			List<Application> appList = configParser.parseApplication();
			for(Application app:appList){
				HettySecurity.addToApplicationMap(app);
			}
			
			List<Service> serviceList = configParser.parseService();
			for(Service service:serviceList){
				ServiceHandler.addToServiceMap(service);
			}
			
			List<ServiceVersion> versionList = configParser.parseSecurity();
			if(versionList != null){
				for(ServiceVersion version:versionList){
					ServiceHandler.addToVersionMap(version);
				}
			}
		}
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

}
