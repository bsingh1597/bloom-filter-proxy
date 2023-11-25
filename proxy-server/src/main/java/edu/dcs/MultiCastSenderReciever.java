package edu.dcs;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.IOException;

public class MultiCastSenderReciever {
    public static void main(String[] args) {
        // Multicast group and port
        String multicastGroup = "224.0.0.1";
        int port = 8888;

        // Message to be sent
        String messageToSend = "Hello, Multicast World!";

        // Create and run the sender and receiver
        new Thread(() -> sendMulticastMessage(multicastGroup, port, messageToSend)).start();
        new Thread(() -> receiveMulticastMessage(multicastGroup, port)).start();
    }

    private static void sendMulticastMessage(String multicastGroup, int port, String message) {
        try {
            InetAddress group = InetAddress.getByName(multicastGroup);
            try (MulticastSocket socket = new MulticastSocket()) {
                // Set the time-to-live for the message
                socket.setTimeToLive(5);

                while (true) {
                    System.out.println("Sending");
                    DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), group, port);
                    socket.send(packet);
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void receiveMulticastMessage(String multicastGroup, int port) {
        try {
            InetAddress group = InetAddress.getByName(multicastGroup);

            try (MulticastSocket socket = new MulticastSocket(port)) {
                // NetworkInterface networkInterface = NetworkInterface.getByName("en0"); //
                // Replace with your network interface name
                socket.joinGroup(new InetSocketAddress(group, port), NetworkInterface.getByName("utun0"));

                while (true) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());
                    System.out.println("Received: " + receivedMessage);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
