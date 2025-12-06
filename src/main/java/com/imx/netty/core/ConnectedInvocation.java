package com.imx.netty.core;

import io.netty.channel.socket.SocketChannel;

public interface ConnectedInvocation {

    void connect(SocketChannel socketChannel);

}
