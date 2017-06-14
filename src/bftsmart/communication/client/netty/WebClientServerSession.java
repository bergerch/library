package bftsmart.communication.client.netty;

import java.nio.channels.Channel;

public class WebClientServerSession {

    private Channel channel;
    private int replicaId;

    public WebClientServerSession(Channel channel, int replicaId) {
        this.channel = channel;
        this.replicaId = replicaId;
    }

    public Channel getChannel() {
        return channel;
    }

    public int getReplicaId() {
        return replicaId;
    }

}




