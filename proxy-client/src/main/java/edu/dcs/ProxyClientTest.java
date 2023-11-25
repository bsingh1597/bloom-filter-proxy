package edu.dcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;

public class ProxyClientTest {
    public static void main(String[] args) {
        int proxyPort = 8083; // Change this to the port of your proxy server

            try {
                // Create a URL object
                URL url;
                try {
                    url = getUrl(proxyPort, "https://www.javatpoint.com/swagger");

                    // Open a connection to the URL
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    // Set the request method to GET
                    connection.setRequestMethod("GET");

                    // Get the response code
                    int responseCode = connection.getResponseCode();
                    System.out.println("Response Code: " + responseCode);

                    // Read the response
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    // StringBuilder response = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        System.out.println("Successfully fetched the response");
                    }

                } catch (SocketException se) {
                    se.printStackTrace();
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static URL getUrl(int portNumber, String internetUrl)
            throws UnknownHostException, UnsupportedEncodingException, MalformedURLException, URISyntaxException {

        StringBuilder urlBuilder = new StringBuilder("http://");
        urlBuilder.append(InetAddress.getLocalHost().getHostAddress());
        urlBuilder.append(":");
        urlBuilder.append(portNumber);
        urlBuilder.append("/proxy");
        urlBuilder.append("?");
        urlBuilder.append(("url="));
        urlBuilder.append(URLEncoder.encode(internetUrl, "UTF-8"));

        return new URI(urlBuilder.toString()).toURL();

    }
}
