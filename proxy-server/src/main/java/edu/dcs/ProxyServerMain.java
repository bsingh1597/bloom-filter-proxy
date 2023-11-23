package edu.dcs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ProxyServerMain
 */
public class ProxyServerMain {

    public static void main(String[] args) {
        int proxyPort = 8083;
        BloomFilter filter = new BloomFilter(50, 2);
        ProxyServer server = new ProxyServerImpl(filter, proxyPort,"224.0.0.1",9876, 15000);
        ServerSocket serverSocket = null;
        try {
            serverSocket = server.createSocket(proxyPort);
            System.out.println("Proxy server running on port " + proxyPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread requestHandler = new Thread(() -> server.handleRequest(clientSocket));
                requestHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}