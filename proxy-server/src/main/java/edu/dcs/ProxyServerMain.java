package edu.dcs;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ProxyServerMain
 */
public class ProxyServerMain {

    public static void main(String[] args) {
        int port = 8082;
        BloomFilter filter = new BloomFilter(50, 2);
        ProxyServer server = new ProxyServerImpl(filter);
        ServerSocket serverSocket = null;
        try {
            serverSocket = server.createSocket(port);
            System.out.println("Proxy server running on port " + port);

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