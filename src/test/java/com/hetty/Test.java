package com.hetty;
import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;

import com.caucho.hessian.client.HessianProxyFactory;
import com.hetty.server.Hello;

public class Test {

	public static void main(String[] args) throws Exception {

		String url = "http://127.0.0.1:9003/hessian/hello/";
		HessianProxyFactory factory = new HessianProxyFactory();
		factory.setUser("client1");

		factory.setPassword("client1");

		factory.setDebug(true);
		factory.setOverloadEnabled(true);
		// factory.setConnectTimeout(timeout);

		// factory.setReadTimeout(100);
		final Hello basic = (Hello) factory.create(Hello.class, url);

		// System.out.println("SayHello:" + basic.hello("guolei"));
		// System.out.println("SayHello:" + basic.test());
		// System.out.println(basic.getAppSecret("11"));
		// User user = basic.getUser(1);
		// System.out.println(user.getRoleList().size());
		// 测试方法重载
		System.out.println(basic.hello("发动机了abc"));
		// System.out.println(basic.hello("guolei"));
		// System.out.println(basic.hello("guolei","hetty"));
		
		long t1 = System.currentTimeMillis();
		int num = 10000;
		
		CountDownLatch signal = new CountDownLatch(1);
		CountDownLatch finish = new CountDownLatch(num);
		
		for (int i=0;i<num;i++) {
			CalcParallelRequestThread client = new CalcParallelRequestThread(basic, signal, finish, 5);
			new Thread(client).start();
		}
		signal.countDown();
		finish.await();
		long t2 = System.currentTimeMillis();
		System.out.println(t2-t1);
	}
}
