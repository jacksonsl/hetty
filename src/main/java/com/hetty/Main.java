package com.hetty;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.hetty.conf.HettyConfig;
import com.hetty.core.HettySecurity;
import com.hetty.core.MetadataProcessor;
import com.hetty.core.NamedThreadFactory;
import com.hetty.object.Application;
import com.hetty.plugin.IPlugin;
import com.hetty.utils.PropertiesUtil;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

public class Main implements Runnable {
	
	private static final Logger LOGGER = Logger.getLogger(Main.class);
	
	protected static PropertiesUtil properties4SysConfig = null;
	private static HettyConfig hettyConfig = HettyConfig.getInstance();
	
	private static int port;
	
	private static boolean SUPPORT_SSL = false;// 是否支持ssl
	
	private static int coreSize = 5;
	private static int maxSize = 100;
	private static int keepAlive = 3000;
	
	static {
		try {
			hettyConfig.loadPropertyFile("server.properties");//default file is this.
			properties4SysConfig = new PropertiesUtil("sysconfig.properties");
			port = Integer.parseInt(properties4SysConfig.getProperty("port"));
			if ("true".equalsIgnoreCase(properties4SysConfig.getProperty("hetty.support.ssl"))) {
				SUPPORT_SSL = true;
			}
			coreSize = Integer.parseInt(properties4SysConfig.getProperty("server.thread.corePoolSize"));
			maxSize = Integer.parseInt(properties4SysConfig.getProperty("server.thread.maxPoolSize"));
			keepAlive = Integer.parseInt(properties4SysConfig.getProperty("server.thread.keepAliveTime"));
		} catch (IOException e) {
			LOGGER.error(e, e);
		}
	}
	
	public Main() {
		
	}

	@Override
	public void run() {
		init();
		EventLoopGroup bossGroup = new NioEventLoopGroup(4);
		EventLoopGroup workerGroup = new NioEventLoopGroup(4);
		try {
			// ssl安全
			SslContext sslCtx = null;
			if (SUPPORT_SSL) {
				LOGGER.info("加载证书...");
				String certFilePath = System.getProperty("user.dir") + File.separator
						+ "ssl" + File.separator + "server.crt";
				String keyFilePath = System.getProperty("user.dir") + File.separator
						+ "ssl" + File.separator + "server_pkcs8.pem";
				File certFile = new File(certFilePath);
				File keyFile = new File(keyFilePath);
		        sslCtx = SslContextBuilder.forServer(certFile, keyFile)
		            .build();
				LOGGER.info("加载证书成功");
			}

			ThreadFactory threadFactory = new NamedThreadFactory("hetty-");
			ExecutorService threadPool = new ThreadPoolExecutor(coreSize, maxSize, keepAlive,
					TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), threadFactory);
	        
			ServerBootstrap serverBootstrap = new ServerBootstrap();
			serverBootstrap.group(bossGroup, workerGroup);
			serverBootstrap.channel(NioServerSocketChannel.class);
			serverBootstrap.childHandler(new HttpChannelInitlalizer(sslCtx, threadPool));
			ChannelFuture f = serverBootstrap.bind(port).sync();
			LOGGER.info("hetty服务器启动成功[port="+port+"]");
			f.channel().closeFuture().sync();
		} catch (Exception e) {
			LOGGER.error("hetty服务器启动失败", e);
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static void main(String[] args) {
		new Main().run();
	}
	
	private void init() {
		initHettySecurity();
		initPlugins();
		initServiceMetaData();
	}
	
	/**
	 * init service metaData
	 */
    private void initHettySecurity() {
    	LOGGER.info("init hetty security...........");
    	Application app = new Application(hettyConfig.getServerKey(), hettyConfig.getServerSecret());
    	HettySecurity.addToApplicationMap(app);
	}
	
    private void initPlugins() {
    	LOGGER.info("init plugins...........");
		List<Class<?>> pluginList = hettyConfig.getPluginClassList();
		try {
			for (Class<?> cls : pluginList) {
				IPlugin p;
				p = (IPlugin) cls.newInstance();
				p.start();
			}
		} catch (InstantiationException e) {
			LOGGER.error("init plugin failed.");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			LOGGER.error("init plugin failed.");
			e.printStackTrace();
		}
	}
    
    private void initServiceMetaData() {
    	LOGGER.info("init service MetaData...........");
    	MetadataProcessor.initMetaDataMap();
	}
}