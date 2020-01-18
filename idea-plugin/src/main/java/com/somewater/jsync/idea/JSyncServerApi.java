package com.somewater.jsync.idea;

import com.intellij.openapi.util.io.StreamUtil;
import com.somewater.jsync.core.conf.HostPort;
import com.somewater.jsync.core.model.ProjectChanges;
import com.somewater.jsync.core.util.RetryException;
import com.somewater.jsync.core.util.SerializationUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Supplier;

public class JSyncServerApi {
    private final HostPort hostPort;
    private final HttpClient httpClient;

    public JSyncServerApi(HostPort hostPort) {
        this.hostPort = hostPort;
        httpClient = HttpClientBuilder
                .create()
                .build();
    }

    public void putChanges(ProjectChanges projectChanges) {
        byte[] payload = SerializationUtil.objectToBytes(projectChanges);
        callWithRetry(() -> putChanges0(payload));
    }

    private void putChanges0(byte[] payload) {
        HttpPut request;
        try {
            request = new HttpPut();
            request.setEntity(new ByteArrayEntity(payload));
            request.setHeader("Content-Type", "application/octet-stream");
            request.setURI(new URI("http", null, hostPort.host, hostPort.port, "/v1/changes",
                    null, null));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        Map.Entry<Integer, String> response = null;
        try {
            response = httpClient.execute(request, httpResponse -> {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                String body = "";
                try {
                body = StreamUtil.readText(httpResponse.getEntity().getContent(), "UTF-8");
                } catch (IOException ignore) {
                }
                return new AbstractMap.SimpleEntry<>(statusCode, body);
            });
        } catch (IOException e) {
            throw new RetryException("Server communication exception", e);
        }
        if (!(response.getKey() < 300 && response.getValue().equals("OK"))) {
            throw new RetryException("Unexpected server response: " + response.getValue());
        }
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
