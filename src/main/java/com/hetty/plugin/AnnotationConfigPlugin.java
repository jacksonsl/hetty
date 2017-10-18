package com.hetty.plugin;

import java.util.List;

import com.hetty.annotation.HessianService;
import com.hetty.conf.HettyConfig;
import com.hetty.core.HettySecurity;
import com.hetty.core.ServiceHandler;
import com.hetty.object.Application;
import com.hetty.object.Service;
import com.hetty.object.ServiceProvider;
import com.hetty.object.ServiceVersion;
import com.hetty.utils.ClassUtil;

public class AnnotationConfigPlugin implements IPlugin {

	@Override
	public boolean start() {
		String scanPackage = HettyConfig.getInstance().getScanPackage();
		if (null == scanPackage) {
			return false;
		}
		List<Class<?>> clazz = ClassUtil.getClasses(scanPackage);
		int i = 0;
		for (Class<?> cla : clazz) {
			i++;
			HessianService hs = cla.getAnnotation(HessianService.class);
			if (null == hs) {
				continue;
			}
			Application app = new Application();
			app.setPassword("client1");
			app.setUser("client1");
			HettySecurity.addToApplicationMap(app);
			Service service = new Service("an"+i, hs.value());
			ServiceProvider sv = new ServiceProvider(hs.version(), cla);
			service.addServiceProvider(hs.version(), sv);
			service.setTypeClass(cla.getInterfaces()[0]);
			service.setDefaultVersion(hs.version());
			service.setOverload(true);
			ServiceHandler.addToServiceMap(service);
			ServiceVersion version = new ServiceVersion();
			version.setService(hs.value());
			version.setVersion(hs.version());
			version.setUser("");
			ServiceHandler.addToVersionMap(version);
		}
		return true;
	}

	@Override
	public boolean stop() {
		return false;
	}

}
