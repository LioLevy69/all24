package org.team100.lib.telemetry;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.junit.jupiter.api.Test;

import edu.wpi.first.wpilibj.Timer;

class UdpTest {
    /**
     * Socket.send
     * 
     * 2.1 us per row (no listener)
     */
    @Test
    void testSocket() throws IOException {
        InetAddress m_addr = InetAddress.getByAddress(new byte[] { 10, 1, 0, 16 });
        // InetAddress m_addr = InetAddress.getLocalHost();
        DatagramSocket m_socket = new DatagramSocket();
        m_socket.setSendBufferSize(100000000);
        byte[] m_bytes = new byte[30];
        DatagramPacket p = new DatagramPacket(m_bytes, 30, m_addr, 1995);
        double t0 = Timer.getFPGATimestamp();
        int N = 1000000;
        for (int i = 0; i < N; ++i) {
            m_socket.send(p);
        }
        m_socket.close();
        double t1 = Timer.getFPGATimestamp();
        System.out.printf("duration sec %5.3f\n", (t1 - t0));
        System.out.printf("duration per row us %5.3f\n", 1000000 * (t1 - t0) / N);
    }

    /**
     * Socket.connect and then socket.send.
     * 
     * 2.1 us per row (no listener)
     */
    @Test
    void testSocketWithConnect() throws IOException {
        InetAddress m_addr = InetAddress.getByAddress(new byte[] { 10, 1, 0, 16 });
        // InetAddress m_addr = InetAddress.getLocalHost();
        DatagramSocket m_socket = new DatagramSocket();
        m_socket.setSendBufferSize(100000000);
        m_socket.connect(m_addr, 1995);
        byte[] m_bytes = new byte[30];
        DatagramPacket p = new DatagramPacket(m_bytes, 30, m_addr, 1995);
        double t0 = Timer.getFPGATimestamp();
        int N = 1000000;
        for (int i = 0; i < N; ++i) {
            m_socket.send(p);
        }
        m_socket.close();
        double t1 = Timer.getFPGATimestamp();
        System.out.printf("duration sec %5.3f\n", (t1 - t0));
        System.out.printf("duration per row us %5.3f\n", 1000000 * (t1 - t0) / N);
    }

    /**
     * Channel.connect and then channel.write (non-blocking)
     * 
     * 2.0 us per row (no listener)
     */
    @Test
    void testChannel() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_SNDBUF, 1000000000);
        InetAddress m_addr = InetAddress.getByAddress(new byte[] { 10, 1, 0, 16 });
        // InetAddress m_addr = InetAddress.getLocalHost();
        InetSocketAddress sockAddr = new InetSocketAddress(m_addr, 1995);
        channel.connect(sockAddr);
        byte[] m_bytes = new byte[30];
        ByteBuffer m_bb = ByteBuffer.wrap(m_bytes);
        double t0 = Timer.getFPGATimestamp();
        int N = 1000000;
        for (int i = 0; i < N; ++i) {
            m_bb.rewind();
            channel.write(m_bb);
        }
        channel.close();
        double t1 = Timer.getFPGATimestamp();
        System.out.printf("duration sec %5.3f\n", (t1 - t0));
        System.out.printf("duration per row us %5.3f\n", 1000000 * (t1 - t0) / N);
    }

    /**
     * Channel.send (non-blocking)
     * 
     * 2.2 us per row (no listener)
     */
    @Test
    void testChannelWithSendAndNotConnect() throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.SO_SNDBUF, 1000000000);
        InetAddress m_addr = InetAddress.getByAddress(new byte[] { 10, 1, 0, 16 });
        // InetAddress m_addr = InetAddress.getLocalHost();
        InetSocketAddress sockAddr = new InetSocketAddress(m_addr, 1995);
        byte[] m_bytes = new byte[30];
        ByteBuffer m_bb = ByteBuffer.wrap(m_bytes);
        double t0 = Timer.getFPGATimestamp();
        int N = 1000000;
        for (int i = 0; i < N; ++i) {
            m_bb.rewind();
            channel.send(m_bb, sockAddr);
        }
        channel.close();
        double t1 = Timer.getFPGATimestamp();
        System.out.printf("duration sec %5.3f\n", (t1 - t0));
        System.out.printf("duration per row us %5.3f\n", 1000000 * (t1 - t0) / N);
    }
}
