package edu.dcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProxyServerImpl
 */
public class ProxyServerImpl implements ProxyServer{

    private static ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<>();

    public ProxyServerImpl(BloomFilter filter) {
    }

    /**
    * Creates server cache.
    *
    * @param port Port on which server will listen.
    */
    @Override
    public ServerSocket createSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    @Override
    public void handleRequest(Socket clientSocket) {
        try {
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String requestLine = clientReader.readLine();
            String[] requestTokens = requestLine.split(" ");
            String method = requestTokens[0];
            String actualUrl = requestTokens[1];

            System.out.println("Request: " + method + " " + actualUrl);

            // Check if url exists in the cache
            String response = cache.get(actualUrl);

            // if response is null then Call internet
            if(response == null) {
                response = fetchRequestFromInternet(actualUrl, method);
                // After fetch from internet put in cache
                appendCache(actualUrl, response);
            }
            
            sendReponseToClient(clientSocket, response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Sends reponse to client.
    *
    * @param clientSocket Accepted socket in the ServerSocket.
    * @param response Text Reponse from the url.
    */
    @Override
    public void sendReponseToClient(Socket clientSocket, String response) {

        try (OutputStream outputStream = clientSocket.getOutputStream()) {
            byte[] responseBytes = response.getBytes();
            outputStream.write(responseBytes);

            // Flush and close the output stream
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void appendCache(String url, String content) {
        cache.put(url,content);
    }

    /**
    * Fetches request from the url.
    *
    * @param urlString Url to call from internet.
    * @param method Method of the API - For now we are using only GET requests.
    * @return Text reponse form internet.
    */
    @Override
    public String fetchRequestFromInternet(String urlString, String method) {
        URL url;
        StringBuilder responseContent = null;
        try {
            url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            // Read the response content
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            responseContent = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        return responseContent != null ? responseContent.toString() : null;
        
    }
    
}