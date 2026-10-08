package com.zzy.petclinic.storage;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.exception.CosClientException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

public class CvmRoleCredentialsProvider implements COSCredentialsProvider {
  private static final Duration REFRESH_AHEAD = Duration.ofMinutes(5);
  private final String roleName;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private volatile CachedCredentials cached;

  public CvmRoleCredentialsProvider(String roleName, ObjectMapper objectMapper) {
    this.roleName = roleName;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
  }

  @Override
  public COSCredentials getCredentials() {
    CachedCredentials current = cached;
    if (current == null || Instant.now().plus(REFRESH_AHEAD).isAfter(current.expiresAt())) {
      synchronized (this) {
        current = cached;
        if (current == null || Instant.now().plus(REFRESH_AHEAD).isAfter(current.expiresAt())) {
          cached = current = fetchCredentials();
        }
      }
    }
    return current.credentials();
  }

  @Override
  public synchronized void refresh() {
    cached = fetchCredentials();
  }

  private CachedCredentials fetchCredentials() {
    try {
      String encodedRole = URLEncoder.encode(roleName, StandardCharsets.UTF_8);
      URI uri =
          URI.create(
              "http://metadata.tencentyun.com/latest/meta-data/cam/security-credentials/"
                  + encodedRole);
      HttpRequest request =
          HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(3)).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
        throw new CosClientException("CVM metadata returned HTTP " + response.statusCode());
      }
      MetadataCredentials value = objectMapper.readValue(response.body(), MetadataCredentials.class);
      if (!"Success".equalsIgnoreCase(value.code())
          || blank(value.secretId())
          || blank(value.secretKey())
          || blank(value.token())) {
        throw new CosClientException("CVM metadata did not return usable temporary credentials");
      }
      Instant expiresAt =
          value.expiredTime() == null
              ? Instant.now().plus(Duration.ofMinutes(30))
              : Instant.ofEpochSecond(value.expiredTime());
      return new CachedCredentials(
          new BasicSessionCredentials(value.secretId(), value.secretKey(), value.token()), expiresAt);
    } catch (IOException e) {
      throw new CosClientException("Failed to parse CVM role credentials", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CosClientException("Interrupted while loading CVM role credentials", e);
    }
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private record CachedCredentials(COSCredentials credentials, Instant expiresAt) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record MetadataCredentials(
      @JsonProperty("Code") String code,
      @JsonProperty("TmpSecretId") String secretId,
      @JsonProperty("TmpSecretKey") String secretKey,
      @JsonProperty("Token") String token,
      @JsonProperty("ExpiredTime") Long expiredTime) {}
}
