package com.proxy.bloom.proxyserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ProxyServer
 */
public interface ProxyServer {

   ServerSocket createSocket(int port) throws IOException;

   void handleRequest(Socket clientSocket);

   void appendCache(String url, String content);

   String fetchRequestFromInternet(String urlString, String method);

   void sendReponseToClient(Socket clientSocket, String response);

}