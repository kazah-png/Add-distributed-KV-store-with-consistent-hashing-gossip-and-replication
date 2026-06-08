package com.kv.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class KvClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public KvClient(String nodeId) {
        // nodeId formato "host:port"
        this.baseUrl = "http://" + nodeId;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void put(String key, String value) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/kv/" + key))
                .header("Content-Type", "text/plain")
                .PUT(HttpRequest.BodyPublishers.ofString(value))
                .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Replica put failed: " + resp.statusCode());
        }
    }

    public void delete(String key) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/kv/" + key))
                .DELETE()
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}