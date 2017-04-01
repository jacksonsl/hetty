package com.hetty;

import java.util.concurrent.ExecutorService;

import com.hetty.core.HettyHandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
 
public class HttpChannelInitlalizer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

	private ExecutorService threadpool;

    public HttpChannelInitlalizer(SslContext sslCtx, ExecutorService threadpool) {
        this.sslCtx = sslCtx;
        this.threadpool = threadpool;
    }
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        if (null != sslCtx) {
            pipeline.addLast(sslCtx.newHandler(ch.alloc()));
        }
		pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpObjectAggregator(1048576));
        pipeline.addLast(new ChunkedWriteHandler());
		pipeline.addLast(new HttpResponseEncoder());
		pipeline.addLast(new HttpContentCompressor());
		pipeline.addLast(new HettyHandler(threadpool));
    }

}