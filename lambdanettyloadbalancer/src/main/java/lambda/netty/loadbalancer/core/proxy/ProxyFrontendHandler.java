/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package lambda.netty.loadbalancer.core.proxy;


import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.AttributeKey;
import lambda.netty.loadbalancer.core.launch.Launcher;
import org.apache.log4j.Logger;

public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {
    final static Logger logger = Logger.getLogger(ProxyFrontendHandler.class);
    public static final AttributeKey DOMAIN = AttributeKey.newInstance("domain");
    Bootstrap b;
    Object requestToProxyServer;
    // As we use inboundChannel.eventLoop() when building the Bootstrap this does not need to be volatile as
    // the outboundChannel will use the same EventLoop (and therefore Thread) as the inboundChannel.
    private Channel outboundChannel;

    public ProxyFrontendHandler() {
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        b = new Bootstrap();
        b = b.group(ctx.channel().eventLoop())
                .channel(ctx.channel().getClass());
        if (Launcher.SCALABILITY_ENABLED) {
            b.handler(new ProxyBackendHandlersInit(channel, System.currentTimeMillis()));
        } else {
            b.handler(new ProxyBackendHandlersInit(channel));
        }


    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {

        requestToProxyServer = msg;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        closeOnFlush(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        if (evt instanceof ProxyEvent) {
            logger.info("Received the event");
            ProxyEvent proxyEvent = (ProxyEvent) evt;
            ChannelFuture f = b.connect(proxyEvent.getHost(), proxyEvent.getPort());
            f.channel().attr(DOMAIN).set(proxyEvent.getDomain());
            f.addListener(new CustomListener(proxyEvent.getHost() + ":" + proxyEvent.getPort()));
        } else {
            System.out.println(evt);
        }
    }

    private final class CustomListener implements ChannelFutureListener {
        private String backendServer;

        CustomListener(String backendServer) {
            this.backendServer = backendServer;
        }

        @Override
        public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (channelFuture.isSuccess()) {
                logger.info("Connected to the backend server: " + backendServer);
                outboundChannel = channelFuture.channel();
                outboundChannel.writeAndFlush(requestToProxyServer);
            }
        }
    }
}