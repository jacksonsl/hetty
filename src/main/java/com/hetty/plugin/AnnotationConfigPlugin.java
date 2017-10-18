package com.hetty.plugin;

import java.util.List;

import com.hetty.conf.AnnotationConfigParser;
import com.hetty.conf.ConfigParser;
import com.hetty.conf.HettyConfig;
import com.hetty.core.HettySecurity;
import com.hetty.core.ServiceHandler;
import com.hetty.object.Application;
import com.hetty.object.Service;
import com.hetty.object.ServiceVersion;

public class AnnotationConfigPlugin implements IPlugin {

	@Override
	public boolean start() {
		String scanPackage = HettyConfig.getInstance().getScanPackage();
		if (null == scanPackage) {
			return false;
		}
		String[] scanPackages = scanPackage.split(",");
		for (String spk : scanPackages) {
			ConfigParser configParser = new AnnotationConfigParser(spk);

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
		return false;
	}

}
