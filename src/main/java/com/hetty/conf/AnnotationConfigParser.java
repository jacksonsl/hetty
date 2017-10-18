package com.hetty.conf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.hetty.annotation.HessianService;
import com.hetty.object.Application;
import com.hetty.object.Service;
import com.hetty.object.ServiceProvider;
import com.hetty.object.ServiceVersion;
import com.hetty.utils.ClassUtil;

public class AnnotationConfigParser implements ConfigParser {

	private static final Logger LOGGER = Logger.getLogger(AnnotationConfigParser.class);
	
	private List<Class<?>> clazzs = null;
	
	public AnnotationConfigParser(String scanPackage) {
		clazzs = ClassUtil.getClasses(scanPackage);
	}

	/**
	 * analyse service configure and return a list,the list is a LocalService and each localService 
	 * corresponding a service
	 */
	@Override
	public List<Service> parseService() {
		List<Service> slist = new ArrayList<Service>();
		if (null != clazzs) {
			for (Class<?> clazz : clazzs) {
				HessianService hs = clazz.getAnnotation(HessianService.class);
				if (null == hs) {
					continue;
				}
				Service service = new Service();
				ServiceProvider sv = new ServiceProvider(hs.version(), clazz);
				service.setName(hs.value());
				service.setId("");
				service.addServiceProvider(hs.version(), sv);
				service.setTypeClass(clazz.getInterfaces()[0]);
				service.setDefaultVersion(hs.version());
				service.setOverload(true);
				slist.add(service);
				LOGGER.info("load service:" + service.getName());
			}
		}
		return slist;
	}

	
	/**
	 * parse application
	 */
	@Override
	public List<Application> parseApplication() {
		List<Application> appList = new LinkedList<Application>();
		Application app = new Application();
		app.setPassword("client1");
		app.setUser("client1");
		appList.add(app);
		return appList;
	}

	/**
	 * get the security info
	 */
	@Override
	public List<ServiceVersion> parseSecurity() {
		List<ServiceVersion> versionList = new LinkedList<ServiceVersion>();
		if (null != clazzs) {
			for (Class<?> clazz : clazzs) {
				HessianService hs = clazz.getAnnotation(HessianService.class);
				if (null == hs) {
					continue;
				}
				ServiceVersion serviceVersion = new ServiceVersion();
				serviceVersion.setUser("");
				serviceVersion.setService(hs.value());
				serviceVersion.setVersion(hs.version());
				versionList.add(serviceVersion);
			}
		}
		return versionList;
	}
}