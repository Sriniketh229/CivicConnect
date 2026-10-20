package com.civicconnect.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** Small backend-only client for the Supabase Storage REST API. */
@Service
public class SupabaseStorageService {
    private final HttpClient client = HttpClient.newHttpClient();
    private final String projectUrl;
    private final String serviceRoleKey;
    private final String bucket;

    public SupabaseStorageService(
            @Value("${supabase.project-url:}") String projectUrl,
            @Value("${supabase.service-role-key:}") String serviceRoleKey,
            @Value("${supabase.storage.bucket:complaint-attachments}") String bucket) {
        this.projectUrl = projectUrl == null ? "" : projectUrl.replaceAll("/+$", "");
        this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey;
        this.bucket = bucket;
    }

    public void upload(String objectPath, byte[] bytes, String mimeType) {
        ensureConfigured();
        HttpRequest request = requestBuilder("/storage/v1/object/" + encode(bucket) + "/" + objectPath)
                .header("Content-Type", mimeType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();
        send(request, "upload");
    }

    public StoredObject download(String objectPath) {
        ensureConfigured();
        HttpRequest request = requestBuilder("/storage/v1/object/" + encode(bucket) + "/" + objectPath)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file was not found.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Attachment storage is unavailable.");
            }
            return new StoredObject(response.body());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Attachment storage is unavailable.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Attachment download was interrupted.", e);
        }
    }

    public void deleteQuietly(String objectPath) {
        if (!isConfigured()) return;
        try {
            client.send(requestBuilder("/storage/v1/object/" + encode(bucket) + "/" + objectPath)
                    .DELETE().build(), HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            // The database transaction remains the source of truth; do not expose storage credentials or details.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private HttpRequest.Builder requestBuilder(String path) {
        return HttpRequest.newBuilder(URI.create(projectUrl + path))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey);
    }

    private void send(HttpRequest request, String operation) {
        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Attachment " + operation + " failed. Check the configured storage bucket.");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Attachment storage is unavailable.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Attachment upload was interrupted.", e);
        }
    }

    private void ensureConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Attachment storage is not configured. Set the Supabase Storage settings on the server.");
        }
    }

    private boolean isConfigured() {
        return projectUrl.startsWith("https://") && !projectUrl.contains("<")
                && !serviceRoleKey.isBlank() && !serviceRoleKey.contains("<") && !bucket.isBlank();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record StoredObject(byte[] bytes) { }
}
