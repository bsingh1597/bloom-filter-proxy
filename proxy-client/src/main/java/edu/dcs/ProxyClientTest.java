package edu.dcs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ProxyClientTest {
    public static void main(String[] args) {
        String proxyHost = "localhost"; // Change this to the address of your proxy server
        int proxyPort = 8082; // Change this to the port of your proxy server

        while (true) {
            try {
                Socket proxySocket = new Socket(proxyHost, proxyPort);

                PrintWriter proxyWriter = new PrintWriter(proxySocket.getOutputStream(), true);
                proxyWriter.println("GET https://www.javatpoint.com/simple-html-pages HTTP/1.1");
                proxyWriter.println(); // Blank line indicates end of headers

                BufferedReader proxyReader = new BufferedReader(new InputStreamReader(proxySocket.getInputStream()));

                // String responseLine;
                while ((proxyReader.readLine()) != null) {
                    // System.out.println("Proxy Response: " + responseLine);
                    System.out.println("Successfully fetched the response");
                }

                proxySocket.close();

            } catch (SocketException se) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            }
            break;
        }
    }
}

