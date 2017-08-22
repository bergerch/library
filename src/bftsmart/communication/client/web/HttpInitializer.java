package bftsmart.communication.client.web;

import bftsmart.communication.client.CommunicationSystemServerSide;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpInitializer extends ChannelInitializer<SocketChannel> {

    CommunicationSystemServerSide communicationSystemServer;

    public HttpInitializer(CommunicationSystemServerSide communicationSystemServer) {
        super();
        this.communicationSystemServer = communicationSystemServer;
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpObjectAggregator(1048576));
        pipeline.addLast("httpHandler", new HttpServerHandler(this.communicationSystemServer));

    }

}