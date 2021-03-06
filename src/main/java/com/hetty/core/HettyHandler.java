package com.hetty.core;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.HessianFactory;
import com.caucho.hessian.io.HessianInputFactory;
import com.caucho.hessian.io.SerializerFactory;
import com.caucho.services.server.ServiceContext;
import com.hetty.object.RequestWrapper;

public class HettyHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = Logger.getLogger(HettyHandler.class);
	
	private FullHttpRequest request;
	private boolean readingChunks;
	private final StringBuilder buf = new StringBuilder();
	private HessianInputFactory _inputFactory = new HessianInputFactory();
	private HessianFactory _hessianFactory = new HessianFactory();

	private SerializerFactory _serializerFactory;
	private ExecutorService threadpool;

	public HettyHandler(ExecutorService threadpool) {
		this.threadpool = threadpool;
	}
	
	private void handleService(final ChannelHandlerContext ctx, final String serviceName, final FullHttpRequest request,
			final ByteArrayOutputStream os)
			throws Exception {
		
		try {
			threadpool.execute(new Runnable() {
				@Override
				public void run() {
					try {
						service(serviceName, request, os);
					} catch (Exception e1) {
						LOGGER.error(e1.getMessage(), e1);
					} finally {
						ReferenceCountUtil.release(request);
					}

					if (HttpUtil.is100ContinueExpected(request)) {
						send100Continue(ctx, request);
					}
					if (HttpUtil.isTransferEncodingChunked(request)) {
						readingChunks = true;
					} else {
						writeResponse(ctx, request, os);
					}

				}
			});
		} catch (RejectedExecutionException exception) {
			LOGGER.error("server threadpool full,threadpool maxsize is:"
					+ ((ThreadPoolExecutor) threadpool).getMaximumPoolSize());
		}
	}

	/**
	 * send response
	 * @param e
	 * @param response
	 * @param os
	 */
	private void writeResponse(final ChannelHandlerContext ctx, FullHttpRequest request, ByteArrayOutputStream os) {

		FullHttpResponse response = null;
		response = new DefaultFullHttpResponse(HTTP_1_1,
				HttpResponseStatus.OK, Unpooled.copiedBuffer(os.toByteArray()));// 解决中文乱码
		
		HttpUtil.setContentLength(response, response.content().readableBytes());
		if (HttpUtil.isKeepAlive(request)) {
			response.headers().set("CONNECTION", HttpHeaderValues.KEEP_ALIVE);
		}
		final ChannelFuture future = ctx.channel().writeAndFlush(response);
		future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture f) {
                assert future == f;
                ctx.close();
            }
        });
	}

	private void send100Continue(ChannelHandlerContext ctx, FullHttpRequest request) {
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
		ByteBuf content = request.content();
		if (content.isReadable()) {
			buf.append(content.toString(CharsetUtil.UTF_8));
		}
		ChannelFuture future = ctx.channel().writeAndFlush(response);
		boolean keepAlive = HttpUtil.isKeepAlive(request);

		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	/**
	 * Sets the serializer factory.
	 */
	public void setSerializerFactory(SerializerFactory factory) {
		_serializerFactory = factory;
	}

	/**
	 * Gets the serializer factory.
	 */
	public SerializerFactory getSerializerFactory() {
		if (_serializerFactory == null)
			_serializerFactory = new SerializerFactory();

		return _serializerFactory;
	}

	/**
	 * Execute a request. The path-info of the request selects the bean. Once
	 * the bean's selected, it will be applied.
	 */
	public void service(String serviceName, FullHttpRequest request, ByteArrayOutputStream os) {
		ByteBuf buf = request.content();  
        final byte[] contentBytes = new byte[buf.readableBytes()];
		LOGGER.info("[可读数据：]"+buf.readableBytes());
        buf.readBytes(contentBytes);
        
		InputStream is = new ByteArrayInputStream(contentBytes);

		SerializerFactory serializerFactory = getSerializerFactory();
		String username = "";
		String password = "";
		String[] authLink = getUsernameAndPassword(request);
		username = authLink[0].equals("")?"":authLink[0];
		password = authLink[1].equals("")?"":authLink[1];
		String clientIP = request.headers().get("Client-IP");
		RequestWrapper rw = new RequestWrapper(username, password, clientIP, serviceName);
		
		invoke(rw, is, os, serializerFactory);
	}

	private String[] getUsernameAndPassword(HttpRequest req) {
		String auths = request.headers().get("Authorization");
		if(auths == null){
			String str[] = {"",""};
			return str;
		}
		String auth[] = auths.split(" ");
		String bauth = auth[1];
		String dauth = new String(Base64.decodeBase64(bauth));
		String authLink[] = dauth.split(":");
		return authLink;
	}

	protected void invoke(RequestWrapper rw, InputStream is, OutputStream os,
			SerializerFactory serializerFactory) {
		AbstractHessianInput in = null;
		AbstractHessianOutput out = null;
		String username = rw.getUser();
		String password = rw.getPassword();
		try {

			HessianInputFactory.HeaderType header = _inputFactory
					.readHeader(is);

			switch (header) {
			case CALL_1_REPLY_1:
				in = _hessianFactory.createHessianInput(is);
				out = _hessianFactory.createHessianOutput(os);
				break;

			case CALL_1_REPLY_2:
				in = _hessianFactory.createHessianInput(is);
				out = _hessianFactory.createHessian2Output(os);
				break;

			case HESSIAN_2:
				in = _hessianFactory.createHessian2Input(is);
				in.readCall();
				out = _hessianFactory.createHessian2Output(os);
				break;

			default:
				throw new IllegalStateException(header
						+ " is an unknown Hessian call");
			}

			if (serializerFactory != null) {
				in.setSerializerFactory(serializerFactory);
				out.setSerializerFactory(serializerFactory);
			}

			/*if (username == null || password == null) {
				Exception exception = new RuntimeException(
						"the client can't offer the user or password infor,please check.");
				out.writeFault("ServiceException", exception.getMessage(),
						exception);
				LOGGER.error("the client can't offer the user or password infor,now we have refused.");
				throw exception;
			}*/
			invoke(rw, in, out);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				out.writeFault("ServiceException", e.getMessage(), e);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} finally {
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	public void invoke(RequestWrapper rw,
			AbstractHessianInput in, AbstractHessianOutput out)
			throws Exception {
		ServiceContext context = ServiceContext.getContext();

		String serviceName = rw.getServiceName();
		
		// backward compatibility for some frameworks that don't read
		// the call type first
		in.skipOptionalCall();

		// Hessian 1.0 backward compatibility
		String header;
		while ((header = in.readHeader()) != null) {
			Object value = in.readObject();

			context.addHeader(header, value);
		}
		ServiceMetaData metaData = MetadataProcessor.getServiceMetaData(serviceName);
		if (metaData == null) {
			LOGGER.error("service " + serviceName+ " can't find.");
			out.writeFault("NoSuchService","service " + serviceName+ " can't find.", null);
			out.close();
			return;
		}
		String methodName = in.readMethod();
		int argLength = in.readMethodArgLength();

		Method method = metaData.getMethod(methodName + "__" + argLength);
		
		if (method == null) {
			method = metaData.getMethod(methodName);
		}
		if (method == null) {
			out.writeFault("NoSuchMethod","service["+methodName+"]'s method " + methodName+ " cannot find", null);
			out.close();
			return;
		}
		Class<?>[] argTypes = method.getParameterTypes();
		Object[] argObjs = new Object[argTypes.length];
		for (int i = 0; i < argTypes.length; i++) {
			argObjs[i] = in.readObject(argTypes[i]);
		}

		//wrap the request to a wapper
		rw.setMethodName(method.getName());
		rw.setArgs(argObjs);
		rw.setArgsTypes(argTypes);

		if (argLength != argObjs.length && argLength >= 0) {
			out.writeFault("NoSuchMethod","service["+methodName+"]'s method " + methodName
							+ " argument length mismatch, received length="
							+ argLength, null);
			out.close();
			return;
		}

		Object result = null;

		try {
			//handle request
			result = ServiceHandler.handleRequest(rw);
		} catch (Exception e) {
			Throwable e1 = e;
			if (e1 instanceof InvocationTargetException)
				e1 = ((InvocationTargetException) e).getTargetException();

			LOGGER.debug(this + " " + e1.toString(), e1);
			result = e;
			out.writeFault("ServiceException", e1.getMessage(), e1);
			out.close();
			return;
		}

		// The complete call needs to be after the invoke to handle a
		// trailing InputStream
		in.completeCall();

		out.writeReply(result);

		out.close();
	}

	protected Hessian2Input createHessian2Input(InputStream is) {
		return new Hessian2Input(is);
	}

	private void sendResourceNotFound(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				HttpResponseStatus.NOT_FOUND, Unpooled.copiedBuffer(HttpResponseStatus.NOT_FOUND.toString(),
						CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.channel().write(response).addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		request = (FullHttpRequest)msg;
		if (!readingChunks) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			String uri = request.uri();
			if (!uri.startsWith("/hessian/")) {
				sendResourceNotFound(ctx);
				return;
			}
			if (uri.endsWith("/")) {
				uri = uri.substring(0, uri.length() - 1);
			}

			String serviceName = uri.substring(uri.lastIndexOf("/") + 1);

			//client ip
			SocketAddress remoteAddress = ctx.channel().remoteAddress();
			String ipAddress = remoteAddress.toString().split(":")[0];
			request.headers().set("Client-IP", ipAddress.substring(1));
			handleService(ctx, serviceName, request, os);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		if (ctx.channel().isActive()) {
			sendError(ctx, INTERNAL_SERVER_ERROR);
		}
		ctx.close();
	}

	private static void sendError(ChannelHandlerContext ctx,
			HttpResponseStatus status) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				status, Unpooled.copiedBuffer("Failure: " + status + "\r\n",
						CharsetUtil.UTF_8));
		response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

		// Close the connection as soon as the error message is sent.
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
}
