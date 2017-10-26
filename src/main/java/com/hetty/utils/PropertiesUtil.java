package com.hetty.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

/**
 * 读取属性配置文件
 * @author sunlei
 * @date 2016年7月26日
 */
public class PropertiesUtil {

	/**
	 * 读取配置文件对象
	 */
	private Properties proper;

	/**
	 * 初始化类
	 * 
	 * @param configName
	 *            配置文件地址路径与名称的组合
	 */
	public PropertiesUtil(String configName) throws IOException {
		try {
			proper = new Properties();
			if (null == configName) {
				throw new IOException();
			}
			String filePath = System.getProperty("user.dir") + File.separator
					+ "config" + File.separator + configName;
			InputStream in = new BufferedInputStream(new FileInputStream(filePath));
			proper.load(in);
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * 方法用途: 获取配置信息<br>
	 * 实现步骤: <br>
	 * 
	 * @param key
	 *            配置信息的键值
	 * @return
	 */
	public String getProperty(String key) {
		return proper.getProperty(key);
	}
	
	public Set<Object> keySet() {
		return proper.keySet();
	}

}
