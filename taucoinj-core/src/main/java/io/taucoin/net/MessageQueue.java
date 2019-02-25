package io.taucoin.net;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.taucoin.listener.TaucoinListener;
import io.taucoin.net.message.Message;
import io.taucoin.net.message.ReasonCode;
import io.taucoin.net.p2p.DisconnectMessage;
import io.taucoin.net.p2p.PingMessage;
import io.taucoin.net.server.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

import static io.taucoin.net.message.StaticMessages.DISCONNECT_MESSAGE;

/**
 * This class contains the logic for sending messages in a queue
 *
 * Messages open by send and answered by receive of appropriate message
 *      PING by PONG
 *      GET_PEERS by PEERS
 *      GET_TRANSACTIONS by TRANSACTIONS
 *      GET_BLOCK_HASHES by BLOCK_HASHES
 *      GET_BLOCKS by BLOCKS
 *
 * The following messages will not be answered:
 *      PONG, PEERS, HELLO, STATUS, TRANSACTIONS, BLOCKS
 *
 * @author Roman Mandeleil
 */
public class MessageQueue {

    private static final Logger logger = LoggerFactory.getLogger("net");

    private static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        private AtomicInteger cnt = new AtomicInteger(0);
        public Thread newThread(Runnable r) {
            return new Thread(r, "MessageQueueTimer-" + cnt.getAndIncrement());
        }
    });

    private Queue<MessageRoundtrip> messageQueue = new ConcurrentLinkedQueue<>();
    private ChannelHandlerContext ctx = null;

    TaucoinListener ethereumListener;
    boolean hasPing = false;
    private ScheduledFuture<?> timerTask;
    private Channel channel;

    @Inject
    public MessageQueue(TaucoinListener listener) {
        this.ethereumListener = listener;
    }

    public void activate(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        timerTask = timer.scheduleAtFixedRate(new Runnable() {
            public void run() {
                try {
                    nudgeQueue();
                } catch (Throwable t) {
                    logger.error("Unhandled exception", t);
                }
            }
        }, 10, 10, TimeUnit.MILLISECONDS);
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void sendMessage(Message msg) {
        if (msg instanceof PingMessage) {
            if (hasPing) return;
            hasPing = true;
        }

        messageQueue.add(new MessageRoundtrip(msg));
    }

    public void disconnect() {
        disconnect(DISCONNECT_MESSAGE);
    }

    public void disconnect(ReasonCode reason) {
        disconnect(new DisconnectMessage(reason));
    }

    private void disconnect(DisconnectMessage msg) {
        ctx.writeAndFlush(msg);
        ctx.close();
    }

    public void receivedMessage(Message msg) throws InterruptedException {

        ethereumListener.trace("[Recv: " + msg + "]");

        if (messageQueue.peek() != null) {
            MessageRoundtrip messageRoundtrip = messageQueue.peek();
            Message waitingMessage = messageRoundtrip.getMsg();

            if (waitingMessage instanceof PingMessage) hasPing = false;

            if (waitingMessage.getAnswerMessage() != null
                    && msg.getClass() == waitingMessage.getAnswerMessage()) {
                messageRoundtrip.answer();
                logger.trace("Message round trip covered: [{}] ",
                        messageRoundtrip.getMsg().getClass());
            }
        }
    }

    private void removeAnsweredMessage(MessageRoundtrip messageRoundtrip) {
        if (messageRoundtrip != null && messageRoundtrip.isAnswered())
            messageQueue.remove();
    }

    private void nudgeQueue() {
        // remove last answered message on the queue
        removeAnsweredMessage(messageQueue.peek());
        // Now send the next message
        sendToWire(messageQueue.peek());
    }

    private void sendToWire(MessageRoundtrip messageRoundtrip) {

        if (messageRoundtrip != null && messageRoundtrip.getRetryTimes() == 0) {
            // TODO: retry logic || messageRoundtrip.hasToRetry()){

            Message msg = messageRoundtrip.getMsg();

            ethereumListener.onSendMessage(channel, msg);

            ctx.writeAndFlush(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);

            if (msg.getAnswerMessage() == null)
                messageQueue.remove();
            else {
                messageRoundtrip.incRetryTimes();
                messageRoundtrip.saveTime();
            }
        }
    }

    public void close() {
        if (timerTask != null) {
            timerTask.cancel(false);
        }
    }
}
