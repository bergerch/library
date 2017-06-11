package bftsmart.communication.client.netty;

import bftsmart.communication.client.CommunicationSystemServerSide;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpInitializer  extends ChannelInitializer<SocketChannel> {

    CommunicationSystemServerSide communicationSystemServer;

    public HttpInitializer(CommunicationSystemServerSide communicationSystemServer) {
        super();
        this.communicationSystemServer = communicationSystemServer;
    }

    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();
        pipeline.addLast("httpServerCodec", new HttpServerCodec());
        pipeline.addLast("httpHandler", new HttpServerHandler(this.communicationSystemServer));

    }

}