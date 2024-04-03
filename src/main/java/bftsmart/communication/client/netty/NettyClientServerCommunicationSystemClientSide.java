/**
 Copyright (c) 2007-2013 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package bftsmart.communication.client.netty;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bftsmart.communication.client.CommunicationSystemClientSide;
import bftsmart.communication.client.ReplyReceiver;
import bftsmart.reconfiguration.ClientViewController;
import bftsmart.tom.core.messages.TOMMessage;
import bftsmart.tom.util.TOMUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GenericFutureListener;

/**
 *
 * @author Paulo
 */
@Sharable
public class NettyClientServerCommunicationSystemClientSide extends SimpleChannelInboundHandler<TOMMessage>
		implements CommunicationSystemClientSide {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private final int clientId;
	protected ReplyReceiver trr;
	// ******* EDUARDO BEGIN **************//
	private ClientViewController controller;
	// ******* EDUARDO END **************//
	private final ConcurrentHashMap<Integer, NettyClientServerSession> sessionClientToReplica = new ConcurrentHashMap<>();
	private ReentrantReadWriteLock rl;
	private Signature signatureEngine;
	private boolean closed = false;

	private final EventLoopGroup workerGroup;
	private SyncListener listener;

	private SecretKeyFactory secretKeyFactory;

	/* Tulio Ribeiro */
	private static final int tcpSendBufferSize = 8 * 1024 * 1024;
	private static final int connectionTimeoutMsec = 40000; /* (40 seconds, timeout) */
	private PrivateKey privateKey;
	/* end Tulio Ribeiro */

	// Used for a re-transmission of the last (pending) request in case of a re-connect to some replica
	private TOMMessage pendingRequest;
	private boolean pendingRequestSign;

	public NettyClientServerCommunicationSystemClientSide(int clientId, ClientViewController controller) {
		super();

		this.clientId = clientId;
		this.workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors());
		try {

			this.secretKeyFactory = TOMUtil.getSecretFactory();

			this.controller = controller;

			/* Tulio Ribeiro */
			privateKey = controller.getStaticConf().getPrivateKey();

			this.listener = new SyncListener();
			this.rl = new ReentrantReadWriteLock();

			int[] currV = controller.getCurrentViewProcesses();

			for (int replicaId : currV) {
				try {
					ChannelFuture future = connectToReplica(replicaId, secretKeyFactory);

					logger.debug("ClientID {}, connecting to replica {}, at address: {}", clientId, replicaId,
							controller.getRemoteAddress(replicaId));

					future.awaitUninterruptibly();

					if (!future.isSuccess()) {
						logger.error("Impossible to connect to " + replicaId);
					}

				} catch (NullPointerException ex) {
					// What is this??? This is not possible!!!
					logger.debug("Should fix the problem, and I think it has no other implications :-), "
							+ "but we must make the servers store the view in a different place.");
				} catch (Exception ex) {
					logger.error("Failed to initialize MAC engine", ex);
				}
			}
		} catch (NoSuchAlgorithmException ex) {
			logger.error("Failed to initialize secret key factory", ex);
		}
	}

	@Override
	public void updateConnections() {
		int[] currV = controller.getCurrentViewProcesses();
		try {
			// open connections with new servers
			for (int replicaId : currV) {
				rl.readLock().lock();
				if (sessionClientToReplica.get(replicaId) == null) {
					rl.readLock().unlock();
					rl.writeLock().lock();
					try {
						ChannelFuture future = connectToReplica(replicaId, secretKeyFactory);
						logger.debug("ClientID {}, updating connection to replica {}, at address: {}", clientId,
								replicaId, controller.getRemoteAddress(replicaId));

						future.awaitUninterruptibly();

						if (!future.isSuccess()) {
							logger.error("Impossible to connect to " + replicaId);
						}

					} catch (InvalidKeyException | InvalidKeySpecException ex) {
						logger.error("Failed to initialize MAC engine", ex);
					}
					rl.writeLock().unlock();
				} else {
					rl.readLock().unlock();
				}
			}
		} catch (NoSuchAlgorithmException ex) {
			logger.error("Failed to initialzie secret key factory", ex);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (cause instanceof ClosedChannelException) {
			logger.error("Connection with replica closed.", cause);
		} else if (cause instanceof ConnectException) {
			logger.error("Impossible to connect to replica.", cause);
		} else if (cause instanceof IOException) {
			logger.error("Replica disconnected. Connection reset by peer.");
		} else {
			logger.error("Replica disconnected.", cause);
		}
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, TOMMessage sm) throws Exception {
		logger.debug("channelRead0(ChannelHandlerContext ctx, TOMMessage sm).");

		if (closed) {
			closeChannelAndEventLoop(ctx.channel());
			return;
		}
		trr.replyReceived(sm);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {

		if (closed) {
			closeChannelAndEventLoop(ctx.channel());
			return;
		}
		logger.debug("Channel active");
	}

	public void reconnect(final ChannelHandlerContext ctx) {
		rl.writeLock().lock();

		ArrayList<NettyClientServerSession> sessions = new ArrayList<>(
				sessionClientToReplica.values());
		for (NettyClientServerSession ncss : sessions) {
			if (ncss.getChannel() == ctx.channel()) {
				int replicaId = ncss.getReplicaId();
				try {
					if (controller.getRemoteAddress(replicaId) != null) {

						ChannelFuture future;
						try {
							future = connectToReplica(replicaId, secretKeyFactory);
							// Re-transmit a request after re-connection
							future.await();
							retransmitMessage(this.pendingRequest, replicaId, this.pendingRequestSign);
						} catch (InvalidKeyException | InvalidKeySpecException e) {
							// TODO Auto-generated catch block
							logger.error("Error in key.",e);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						logger.info("ClientID {}, re-connection to replica {}, at address: {}", clientId, replicaId,
								controller.getRemoteAddress(replicaId));

					} else {
						// This cleans an old server from the session table
						removeClient(replicaId);
					}
				} catch (NoSuchAlgorithmException ex) {
					logger.error("Failed to reconnect to replica", ex);
				}
			}
		}

		rl.writeLock().unlock();
	}

	@Override
	public void setReplyReceiver(ReplyReceiver trr) {
		this.trr = trr;
	}

	@Override
	public void send(boolean sign, int[] targets, TOMMessage sm, Map<Integer, byte[]> replicaSpecificContents) {
		int quorum = controller.getReplyQuorum();
		Integer[] targetArray = Arrays.stream(targets).boxed().toArray(Integer[]::new);
		Collections.shuffle(Arrays.asList(targetArray), new Random());


		listener.waitForChannels(quorum); // wait for the previous transmission to complete

		logger.debug("Sending request from {} with sequence number {} to {}", sm.getSender(), sm.getSequence(),
				Arrays.toString(targetArray));

		this.pendingRequest = sm;
		this.pendingRequestSign = sign;

		if (sm.serializedMessage == null) {
			//sender(int) + viewID(int) + type(byte) + session(int) + sequence(int) + operationId(int)
			// + replyServer(int) + commonContent.length(int) + commonContent(bytes) + metadata(byte)
			int dataLength = Integer.BYTES * 7 + Byte.BYTES * 2
					+ (sm.getCommonContent() == null ? 0 : sm.getCommonContent().length);
			// serialize message
			sm.getSerializedMessage();//this call initializes sm.serializedMessage
		}

		// produce signature
		if (sign && sm.serializedMessageSignature == null) {
			sm.serializedMessageSignature = signMessage(privateKey, sm.serializedMessage);
		}

		int sent = 0;

		for (int target : targetArray) {
			// This is done to avoid a race condition with the writeAndFlush method.
			// Since the method is asynchronous, each iteration of this loop could overwrite
			// the destination of the previous one
			sm = sm.createCopy();

			if (replicaSpecificContents != null) {
				sm.setReplicaSpecificContent(replicaSpecificContents.get(target));
			}

			sm.destination = targets[target];

			rl.readLock().lock();
			Channel channel = sessionClientToReplica.get(targets[target]).getChannel();
			rl.readLock().unlock();
			if (channel.isActive()) {
				sm.signed = sign;
				ChannelFuture f = channel.writeAndFlush(sm);

				f.addListener(listener);

				sent++;
			} else {
				logger.debug("Channel to {} is not connected", targets[target]);
			}
		}

		if (targets.length > controller.getCurrentViewF() && sent < controller.getCurrentViewF() + 1) {
			// if less than f+1 servers are connected send an exception to the client
			throw new RuntimeException("Impossible to connect to servers!");
		}
		if (targets.length == 1 && sent == 0)
			throw new RuntimeException("Server not connected");
	}

	public void sign(TOMMessage sm) {
		// serialize message
		if (sm.serializedMessage == null) {
			sm.getSerializedMessage();// this call initializes sm.serializedMessage
		}
		if (sm.serializedMessageSignature == null) {
			// produce signature
			sm.serializedMessageSignature = signMessage(privateKey, sm.serializedMessage);
		}
	}

	public byte[] signMessage(PrivateKey key, byte[] message) {
		try {
			if (signatureEngine == null) {
				signatureEngine = TOMUtil.getSigEngine();
			}
			signatureEngine.initSign(key);
			signatureEngine.update(message);

			return signatureEngine.sign();
		} catch (Exception e) {
			logger.error("Failed to sign message", e);
			return null;
		}
	}

	@Override
	public void close() {
		this.closed = true;
		// Iterator sessions = sessionClientToReplica.values().iterator();
		rl.readLock().lock();
		ArrayList<NettyClientServerSession> sessions = new ArrayList<>(sessionClientToReplica.values());
		rl.readLock().unlock();
		for (NettyClientServerSession ncss : sessions) {
			Channel c = ncss.getChannel();
			closeChannelAndEventLoop(c);
		}
	}

	private ChannelInitializer<SocketChannel> getChannelInitializer() {

		NettyClientPipelineFactory nettyClientPipelineFactory = new NettyClientPipelineFactory(this,
				sessionClientToReplica, controller, rl);

		return new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) {
				ch.pipeline().addLast(nettyClientPipelineFactory.getDecoder());
				ch.pipeline().addLast(nettyClientPipelineFactory.getEncoder());
				ch.pipeline().addLast(nettyClientPipelineFactory.getHandler());
			}
		};
	}

	@Override
	public void channelUnregistered(final ChannelHandlerContext ctx) {
		scheduleReconnect(ctx, 10);
	}

	@Override
	public void channelInactive(final ChannelHandlerContext ctx) {
		scheduleReconnect(ctx, 10);
	}

	private void closeChannelAndEventLoop(Channel c) {
		// once having an event in your handler (EchoServerHandler)
		// Close the current channel
		c.close();
		// Then close the parent channel (the one attached to the bind)
		if (c.parent() != null) {
			c.parent().close();
		}
		workerGroup.shutdownGracefully();
	}

	private void scheduleReconnect(final ChannelHandlerContext ctx, int time) {
		if (closed) {
			closeChannelAndEventLoop(ctx.channel());
			return;
		}

		final EventLoop loop = ctx.channel().eventLoop();
		loop.schedule(new Runnable() {
			@Override
			public void run() {
				reconnect(ctx);
			}
		}, time, TimeUnit.SECONDS);
	}

	private class SyncListener implements GenericFutureListener<ChannelFuture> {

		private int remainingFutures;

		private final Lock futureLock;
		private final Condition enoughCompleted;

		public SyncListener() {

			this.remainingFutures = 0;

			this.futureLock = new ReentrantLock();
			this.enoughCompleted = futureLock.newCondition();
		}

		@Override
		public void operationComplete(ChannelFuture f) {

			this.futureLock.lock();

			this.remainingFutures--;

			if (this.remainingFutures <= 0) {

				this.enoughCompleted.signalAll();
			}

			logger.debug(this.remainingFutures + " channel operations remaining to complete");

			this.futureLock.unlock();

		}

		public void waitForChannels(int n) {

			this.futureLock.lock();
			if (this.remainingFutures > 0) {

				logger.debug("There are still " + this.remainingFutures
						+ " channel operations pending, waiting to complete");

				try {
					this.enoughCompleted.await(1000, TimeUnit.MILLISECONDS); // timeout if a malicious replica refuses to
					// acknowledge the operation as
					// completed
				} catch (InterruptedException ex) {
					logger.error("Interruption while waiting on condition", ex);
				}

			}

			logger.debug("All channel operations completed or timed out");

			this.remainingFutures = n;

			this.futureLock.unlock();
		}

	}

	/**
	 * Tulio Ribeiro Connect to specific replica and returns the ChannelFuture.
	 * sessionClientToReplica is replaced with the new connection. Removed redundant
	 * code.
	 */
	public synchronized ChannelFuture connectToReplica(int replicaId, SecretKeyFactory fac)
			throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {

		String str = this.clientId + ":" + replicaId;
		PBEKeySpec spec = TOMUtil.generateKeySpec(str.toCharArray());
		SecretKey authKey = fac.generateSecret(spec);

		Bootstrap b = new Bootstrap();
		b.group(workerGroup);
		b.channel(NioSocketChannel.class);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_SNDBUF, tcpSendBufferSize);
		b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMsec);
		b.handler(getChannelInitializer());

		ChannelFuture channelFuture = b.connect(controller.getRemoteAddress(replicaId));

		NettyClientServerSession ncss = new NettyClientServerSession(
				channelFuture.channel(), replicaId);
		sessionClientToReplica.put(replicaId, ncss);

		return channelFuture;
	}

	public synchronized void removeClient(int clientId) {
		sessionClientToReplica.remove(clientId);
	}

	/**
	 * Re-transmits a pending request to a recovered replica after successful re-connection
	 *
	 * @param sm pending TOM Message
	 * @param replicaId recovered replica's id
	 * @param sign if a signature should be added
	 */
	private void retransmitMessage(TOMMessage sm, int replicaId, boolean sign) {
		// No pending request then abort;
		if (sm == null) {
			return;
		}
		logger.debug("Re-transmitting request from " + sm.getSender() + " with sequence number " + sm.getSequence() + " to "
				+ replicaId);
		if (sm.serializedMessage == null) {
			sm.getSerializedMessage(); //this call initializes sm.serializedMessage
		}
		if (sign && sm.serializedMessageSignature == null) {
			sm.serializedMessageSignature = signMessage(privateKey, sm.serializedMessage);
		}
		try {
			sm = (TOMMessage) sm.createCopy();
		} catch (Exception e) {
			logger.error("Failed to clone TOMMessage", e);
		}
		sm.destination = replicaId;
		rl.readLock().lock();
		Channel channel = sessionClientToReplica.get(replicaId).getChannel();
		rl.readLock().unlock();
		if (channel.isActive()) {
			sm.signed = sign;
			logger.debug("-- Write and Flush " + sm.getSequence());
			ChannelFuture f = channel.writeAndFlush(sm);
			f.addListener(listener);
		} else {
			logger.debug("Channel to " + replicaId + " is not connected");
		}
	}
}
