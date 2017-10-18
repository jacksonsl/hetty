package com.hetty.conf;

import io.netty.util.internal.StringUtil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.hetty.object.HettyException;


/**
 * @author guolei
 *
 */
public class HettyConfig {
	private static final Logger LOGGER = Logger.getLogger(HettyConfig.class);
	
	private static Properties properties = new Properties();
	private static HettyConfig instance = null;
	
	private HettyConfig(){
		
	}

	/**
	 * return instance
	 * @return
	 */
	public  static HettyConfig getInstance() {
		if (instance == null) {
			instance = new HettyConfig();
		}
		return instance;
	}
	/**
	 * load config file
	 * Example: loadPropertyFile("server.properties");
	 * @param file class path
	 */
	public void loadPropertyFile(String file) {
		if (StringUtil.isNullOrEmpty(file)) {
			throw new IllegalArgumentException(
					"Parameter of file can not be blank");
		}
		if (file.contains("..")) {
			throw new IllegalArgumentException(
					"Parameter of file can not contains \"..\"");
		}
		String filePath = System.getProperty("user.dir") + File.separator
				+ "config" + File.separator + file;
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(filePath));
			properties.load(inputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			LOGGER.error("error:", e);
			throw new IllegalArgumentException("Properties file not found: "
					+ file);
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("error:", e);
			throw new IllegalArgumentException(
					"Properties file can not be loading: " + file);
		} finally {
			try {
				if (inputStream != null)
					inputStream.close();
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		if (properties == null)
			throw new RuntimeException("Properties file loading failed: "+ file);
	}
	
	public String getProperty(String key) {
		return properties.getProperty(key).trim();
	}

	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue.trim());
	}

	public Properties getProperties() {
		return properties;
	}
	
	/**
	 * get server key
	 * @return server key
	 */
	public String getServerKey() {
		String serverKey = properties.getProperty("server.key");
		if (serverKey == null) {
			throw new RuntimeException("we cannot find the server.key,please check and add.");
		}
		return serverKey.trim();
	}

	/**
	 * get server secret
	 * @return server secret
	 */
	public String getServerSecret() {
		String serverSecret = properties.getProperty("server.secret");
		if (serverSecret == null) {
			throw new RuntimeException("we cannot find the server.secret,please check and add.");
		}
		return serverSecret.trim();
	}

	/**
	 * get the service properties file,default is config.xml
	 * @return
	 */
	public String getpropertiesFile() {
		String f = properties.getProperty("properties.file", "config.xml");
		return f.trim();
	}

	/**
	 * get the server's connect timeout,default is 3s
	 * @return
	 */
	public int getConnectionTimeout() {
		String timeOutStr = properties.getProperty("server.connection.timeout",
				"3000");
		return Integer.parseInt(timeOutStr);
	}

	/**
	 * get the method's invoke timeout,default is 3s
	 * @return
	 */
	public int getMethodTimeout() {
		String timeOutStr = properties.getProperty("server.method.timeout", "3000");
		return Integer.parseInt(timeOutStr);
	}

	/**
	 * get the server's HTTP port,default is -1
	 * @return
	 */
	public int getHttpPort() {
		String port = properties.getProperty("server.http.port", "-1");
		return Integer.parseInt(port);
	}
	/**
	 * get the server's HTTPS port,default is -1
	 * @return
	 */
	public int getHttpsPort() {
		String port = properties.getProperty("server.https.port", "-1");
		return Integer.parseInt(port);
	}

	/**
	 * get the ssl client auth
	 * @return
	 */
	public String getClientAuth() {
		return properties.getProperty("ssl.clientAuth", "none").trim();
	}
	/**
	 * get the ssl keystore file path
	 * @return
	 */
	public String getKeyStorePath() {
		return properties.getProperty("ssl.keystore.file");
	}
	/**
	 * get the ssl keystore password
	 * @return
	 */
	public String getKeyStorePassword() {
		return properties.getProperty("ssl.keystore.password").trim();
	}
	/**
	 * get the ssl certificate key file path
	 * @return
	 */
	public String getCertificateKeyFile() {
		return properties.getProperty("ssl.certificate.key.file").trim();
	}
	/**
	 * get the ssl certificate  file path
	 * @return
	 */
	public String getCertificateFile() {
		return properties.getProperty("ssl.certificate.file").trim();
	}
	/**
	 * get the ssl certificate password
	 * @return
	 */
	public String getCertificatePassword() {
		return properties.getProperty("ssl.certificate.password").trim();
	}
	/**
	 * get the hessian scan package
	 * @return
	 */
	public String getScanPackage() {
		String spk = properties.getProperty("server.hessian.scanPackage");
		return (null == spk )? null : spk.trim();
	}
	/**
	 * get the core number of threads
	 * @return
	 */
	public int getServerCorePoolSize(){
		String coreSize = properties.getProperty("server.thread.corePoolSize", "4");
		return Integer.parseInt(coreSize);
	}
	/**
	 * get the maximum allowed number of threads
	 * @return
	 */
	public int getServerMaximumPoolSize(){
		String maxSize = properties.getProperty("server.thread.maxPoolSize","16");
		return Integer.parseInt(maxSize);
	}
	/**
	 * get the thread keep-alive time, which is the amount of time that threads in excess of the core pool size may remain idle before being terminated
	 * @return
	 */
	public int getServerKeepAliveTime(){
		String aleveTime = properties.getProperty("server.thread.keepAliveTime", "3000");
		return Integer.parseInt(aleveTime);
	}

	/**
	 * get the plugin list which have configured in the server config file
	 * 
	 * @return config class List
	 */
	public List<Class<?>> getPluginClassList() {

		Set<String> keySet = properties.stringPropertyNames();
		List<Class<?>> list = new ArrayList<Class<?>>();
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		try {
			for (String key : keySet) {
				if (key.startsWith("server.plugin")) {
					String cls = properties.getProperty(key);
					Class<?> cls1 = classLoader.loadClass(cls);
					list.add(cls1);
				}
			}
			if (list.size() == 0) {
				Class<?> xmlConfigPlugin = classLoader
						.loadClass("com.hetty.plugin.XmlConfigPlugin");
				Class<?> annotationConfigPlugin = classLoader
						.loadClass("com.hetty.plugin.AnnotationConfigPlugin");
				list.add(xmlConfigPlugin);
				list.add(annotationConfigPlugin);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new HettyException(
					"load plugin class failed.please check your plugin config.");
		}
		return list;
	}
	/**
	 * get develop mod,default is false
	 */
	public boolean getDevMod(){
		String dev = properties.getProperty("server.devmod", "false");
		return Boolean.parseBoolean(dev);
	}
}
