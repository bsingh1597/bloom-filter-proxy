package edu.dcs;

import java.io.BufferedReader;
import java.io.FileReader;
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
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

public class ProxyClientTest {
    public static void main(String[] args) {

        List<Integer> proxyPort = new ArrayList<>();
        proxyPort.add(8082);
        proxyPort.add(8083);
        proxyPort.add(8084);

        RoundRobinIterator<Integer> roundRobinIterator = new RoundRobinIterator<>(proxyPort);

        String filePath = "/Users/bsingh/Documents/College/Advanced DCS/Course Project/bloom-filter-proxy/proxy-client/src/main/resources/url-list.csv"; 

        try (CSVReader csvReader = new CSVReader(new FileReader(filePath))) {
            List<String[]> records = csvReader.readAll();

            records.forEach(url -> {
                callProxyServer(roundRobinIterator.iterator().next(), url[0]);
            });

        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }

        System.out.println("Completed with all the URLS");
    }

    private static void callProxyServer(Integer proxyPort, String stringUrl) {
        try {
            URL url;
            try {
                System.out.println("POrt: "+proxyPort+" Calling URL: "+stringUrl);
                url = getUrl(proxyPort, stringUrl);

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
                    break;
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
