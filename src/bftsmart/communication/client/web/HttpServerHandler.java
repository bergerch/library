package bftsmart.communication.client.web;

import bftsmart.communication.client.CommunicationSystemServerSide;
import bftsmart.communication.client.netty.NettyClientServerCommunicationSystemServerSide;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static io.netty.buffer.Unpooled.copiedBuffer;

public class HttpServerHandler extends WebClientHandler {

    private WebSocketServerHandshaker handshaker;
    private HttpRequest httpRequest;

    public HttpServerHandler(CommunicationSystemServerSide communicationSystemServer) {
        super((NettyClientServerCommunicationSystemServerSide) communicationSystemServer);

    }

    @Override
    public void sendTo(WebClientServerSession clientSession, String jsonMsg) {
        FullHttpResponse response = new BFTFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                copiedBuffer(jsonMsg.getBytes()), httpRequest, jsonMsg.length()
        );
        clientSession.getCtx().writeAndFlush(response);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof HttpRequest || msg instanceof FullHttpMessage) {

            HttpRequest httpRequest = (HttpRequest) msg;
            this.httpRequest = httpRequest;
            HttpHeaders headers = httpRequest.headers();

            // Upgrade to Websocket connection
            if ((this.config.getUseWebsockets() &&
                    (headers.get("Connection") != null && headers.get("Connection").equalsIgnoreCase("Upgrade")) ||
                    (headers.get("Upgrade") != null && headers.get("Upgrade").equalsIgnoreCase("WebSocket")))) {

                System.out.println("Connection : " + headers.get("Connection"));
                System.out.println("Upgrade : " + headers.get("Upgrade"));

                //Adding new handler to the existing pipeline to handle WebSocket Messages
                ctx.pipeline().replace(this, "websocketHandler", new WebSocketHandler(communicationServer));

                System.out.println("WebSocketHandler added to the pipeline");
                System.out.println("Opened Channel : " + ctx.channel());
                System.out.println("Handshaking....");
                //Do the Handshake to upgrade connection from HTTP to WebSocket protocol
                try {
                    handleHandshake(ctx, httpRequest);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                System.out.println("Handshake is done");

            } else { // Handle HTTP Request

                if (msg instanceof FullHttpRequest) {
                    ByteBuf jsonBuf = ((FullHttpRequest) msg).content();
                    String jsonStr = jsonBuf.toString(CharsetUtil.UTF_8).replaceAll("\n", "").replaceAll("\t", "").replaceAll(" ", "");
                    this.readMessage(ctx, jsonStr);
                }
            }
        }
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
                                Throwable cause) throws Exception {
        ctx.writeAndFlush(new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                copiedBuffer(cause.getMessage().getBytes())
        ));
    }

    /* Do the handshaking for WebSocket request */
    protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) throws URISyntaxException {
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketURL(req),
                null, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }


    protected String getWebSocketURL(HttpRequest req) {
        System.out.println("Req URI : " + req.getUri());
        String url = "ws://" + req.headers().get("Host") + req.getUri();
        System.out.println("Constructed URL : " + url);
        return url;
    }


    public class BFTFullHttpResponse extends DefaultFullHttpResponse {

        public BFTFullHttpResponse(HttpVersion version, HttpResponseStatus status, ByteBuf content, HttpRequest httpRequest, int length) {
            super(version, status, content);
            if (httpRequest != null && httpRequest.headers() != null && HttpHeaders.isKeepAlive(httpRequest)) {
                this.headers().set(
                        HttpHeaders.Names.CONNECTION,
                        HttpHeaders.Values.KEEP_ALIVE
                );
            }
            List<String> allowMethods = new ArrayList<>();
            allowMethods.add("GET");
            allowMethods.add("POST");
            this.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);
            this.headers().set(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            this.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
            this.headers().set(HttpHeaders.Names.CONTENT_LENGTH, length);
        }

    }

}