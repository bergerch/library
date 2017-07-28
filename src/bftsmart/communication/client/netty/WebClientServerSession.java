package bftsmart.communication.client.netty;

import io.netty.channel.ChannelHandlerContext;

public class WebClientServerSession {

    private ChannelHandlerContext ctx;
    private int replicaId;
    private int clientId;


    public WebClientServerSession(ChannelHandlerContext ctx, int replicaId, int clientId) {
        this.ctx = ctx;
        this.replicaId = replicaId;
        this.clientId = clientId;
    }


    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WebClientServerSession that = (WebClientServerSession) o;

        if (replicaId != that.replicaId) return false;
        return ctx != null ? ctx.equals(that.ctx) : that.ctx == null;
    }

    @Override
    public int hashCode() {
        int result = ctx != null ? ctx.hashCode() : 0;
        result = 31 * result + replicaId;
        return result;
    }

    public int getReplicaId() {
        return replicaId;
    }

    public int getClientId() {
        return clientId;
    }

    @Override
    public String toString() {
        return "{ReplicaId: " + replicaId + ", Channel: " + ctx.name() + "}";
    }

}




