package com.somewater.jsync.client.network;

import com.somewater.jsync.core.conf.SharedConf;
import com.somewater.jsync.core.model.ProjectChanges;
import com.somewater.jsync.core.model.ProjectChangesResponse;
import com.somewater.jsync.core.util.RetryException;
import com.somewater.jsync.core.util.SerializationUtil;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

public class ServerApi {
    private final String host;
    private final int port;
    private final HttpClient httpClient;
    private static final Duration ConnectTimeout = Duration.ofSeconds(3);
    private static final Duration RequestTimeout = Duration.ofSeconds(30);

    public ServerApi(String host, int port) {
        this.host = host;
        this.port = port;
        httpClient = HttpClient.newBuilder().connectTimeout(ConnectTimeout).build();
    }

    public void putChanges(ProjectChanges projectChanges) {
        callWithRetry(() -> putChanges0(projectChanges));
    }

    private void putChanges0(ProjectChanges projectChanges) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .PUT(HttpRequest.BodyPublishers.ofInputStream(() ->
                            new ByteArrayInputStream(SerializationUtil.objectToBytes(projectChanges))))
                    .setHeader("Content-Type", "application/octet-stream")
                    .uri(new URI("http", null, host, port, "/v1/changes", null, null))
                    .timeout(RequestTimeout)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpResponse<String> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new RetryException("Server communication exception", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!(response.statusCode() < 300 && response.body().equals("OK"))) {
            String truncatedBody = response.body() == null ? "null" :
                    (response.body().length() > 50 ? response.body().subSequence(0, 50) + "..." : response.body());
            throw new RetryException("Unexpected server response: " + truncatedBody);
        }
    }

    public Optional<ProjectChanges> getChanges(String uid, String projectName) {
        return callWithRetry(() -> getChanges0(uid,projectName));
    }

    private Optional<ProjectChanges> getChanges0(String uid, String projectName) {
        HttpRequest request;
        try {
            String path = String.format("/v1/changes/%s/%s", uid, projectName);
            request = HttpRequest.newBuilder()
                    .GET()
                    .uri(new URI("http", null, host, port, path, null, null))
                    .timeout(RequestTimeout)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        HttpResponse<byte[]> response = null;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        ProjectChanges projectChanges = null;
        try {
            ProjectChangesResponse projectChangesResponse = SerializationUtil.bytesToObject(response.body());
            if (projectChangesResponse.errorMessage != null) {
                if (projectChangesResponse.errorMessage.equals(SharedConf.ERROR_MSG_READONLY_SERVER)) {
                    return Optional.empty();
                } else {
                    throw new RuntimeException("Server respond with error: " + projectChangesResponse.errorMessage);
                }
            }
            projectChanges = projectChangesResponse.projectChanges;
        } catch (IOException | ClassNotFoundException e) {
            throw new RetryException("Server response parsing exception", e);
        }
        return Optional.of(projectChanges);
    }

    private static <T> T callWithRetry(Supplier<T> supplier) {
        int cnt = 0;
        while (true) {
            cnt++;
            try {
                return supplier.get();
            } catch (RetryException e) {
                if (cnt > 100) {
                    throw e;
                } else {
                    try {
                        Thread.sleep((long) (1000 * Math.log(cnt)));
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }

    private static void callWithRetry(Runnable runnable) {
        callWithRetry(() -> {
            runnable.run();
            return null;
        });
    }
}
