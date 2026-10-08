package com.zzy.petclinic.storage;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public final class ImageResponses {
  private ImageResponses() {}

  public static ResponseEntity<StreamingResponseBody> stream(StoredImage image) {
    StreamingResponseBody body =
        outputStream -> {
          try (image) {
            image.inputStream().transferTo(outputStream);
          }
        };
    ResponseEntity.BodyBuilder response =
        ResponseEntity.ok()
            .contentType(safeMediaType(image.contentType()))
            .cacheControl(CacheControl.noCache().cachePrivate());
    if (image.contentLength() >= 0) response.contentLength(image.contentLength());
    if (image.etag() != null && !image.etag().isBlank()) response.eTag(image.etag());
    return response.body(body);
  }

  private static MediaType safeMediaType(String contentType) {
    try {
      MediaType mediaType = MediaType.parseMediaType(contentType);
      return "image".equals(mediaType.getType()) ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
    } catch (RuntimeException ignored) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}
