// Copyright (c) 2005 Brian Wellington (bwelling@xbill.org)
/*
 * *
 *  * Copyright 2000-2011 NeuStar, Inc. All rights reserved.
 *  * NeuStar, the Neustar logo and related names and logos are registered
 *  * trademarks, service marks or tradenames of NeuStar, Inc. All other
 *  * product names, company names, marks, logos and symbols may be trademarks
 *  * of their respective owners.
 *
 */

package biz.neustar.hopper.resolver;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public final class TCPClientImpl extends Client implements TCPClient {

    public TCPClientImpl(long endTime, long connectEndTime) throws IOException {
        super(SocketChannel.open(), endTime, connectEndTime);
    }

    public void bind(SocketAddress addr) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        channel.socket().bind(addr);
    }

    public void connect(SocketAddress addr) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.connect(addr)) {
            return;
        }
        key.interestOps(SelectionKey.OP_CONNECT);
        try {
            while (!channel.finishConnect()) {
                if (!key.isConnectable()) {
                    blockUntil(key, connectEndTime);
                }
            }
        } finally {
            if (key.isValid()) {
                key.interestOps(0);
            }
        }
    }

    public void send(byte[] data) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        verboseLog("TCP write", data);
        byte[] lengthArray = new byte[2];
        lengthArray[0] = (byte) (data.length >>> 8);
        lengthArray[1] = (byte) (data.length & 0xFF);
        ByteBuffer[] buffers = new ByteBuffer[2];
        buffers[0] = ByteBuffer.wrap(lengthArray);
        buffers[1] = ByteBuffer.wrap(data);
        int nsent = 0;
        key.interestOps(SelectionKey.OP_WRITE);
        try {
            while (nsent < data.length + 2) {
                if (key.isWritable()) {
                    long n = channel.write(buffers);
                    if (n < 0) {
                        throw new EOFException();
                    }
                    nsent += (int) n;
                    if (nsent < data.length + 2
                            && System.currentTimeMillis() > endTime) {
                        throw new SocketTimeoutException();
                    }
                } else {
                    blockUntil(key, endTime);
                }
            }
        } finally {
            if (key.isValid()) {
                key.interestOps(0);
            }
        }
    }

    private byte[] _recv(int length) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        int nrecvd = 0;
        byte[] data = new byte[length];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        key.interestOps(SelectionKey.OP_READ);
        try {
            while (nrecvd < length) {
                if (key.isReadable()) {
                    long n = channel.read(buffer);
                    if (n < 0) {
                        throw new EOFException();
                    }
                    nrecvd += (int) n;
                    if (nrecvd < length && System.currentTimeMillis() > endTime) {
                        throw new SocketTimeoutException();
                    }
                } else {
                    blockUntil(key, endTime);
                }
            }
        } finally {
            if (key.isValid()) {
                key.interestOps(0);
            }
        }
        return data;
    }

    public byte[] recv() throws IOException {
        byte[] buf = _recv(2);
        int length = ((buf[0] & 0xFF) << 8) + (buf[1] & 0xFF);
        byte[] data = _recv(length);
        verboseLog("TCP read", data);
        return data;
    }

    static byte[] sendrecv(SocketAddress local, SocketAddress remote,
            byte[] data, long endTime, long connectEndTime) throws IOException {
        TCPClientImpl client = new TCPClientImpl(endTime, connectEndTime);
        try {
            if (local != null) {
                client.bind(local);
            }
            client.connect(remote);
            client.send(data);
            return client.recv();
        } finally {
            client.cleanup();
        }
    }

    static byte[] sendrecv(SocketAddress addr, byte[] data, long endTime,
            long connectEndTime) throws IOException {
        return sendrecv(null, addr, data, endTime, connectEndTime);
    }

}