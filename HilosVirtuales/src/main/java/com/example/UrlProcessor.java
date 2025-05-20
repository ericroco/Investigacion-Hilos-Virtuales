package com.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

public class UrlProcessor {

    public static int process(String urlString) throws IOException {
        try {
            URL url = new URI(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                System.out.println(urlString + " no devolvió 200 OK.");
                return -1;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder html = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
            reader.close();

            Set<String> foundUrls = extractUrls(html.toString(), url.getHost());

            return foundUrls.size();

        } catch (Exception e) {
            throw new IOException("No se pudo procesar: " + urlString, e);
        }
    }

    private static Set<String> extractUrls(String html, String host) {
        Set<String> result = new HashSet<>();
        Pattern pattern = Pattern.compile("href\\s*=\\s*\"([^\"]+)\"|src\\s*=\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);

        while (matcher.find()) {
            String link = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);

            if (link.startsWith("http")) {
                result.add(link);
            } else if (link.startsWith("/")) {
                result.add("https://" + host + link);
            }
        }

        return result;
    }
}