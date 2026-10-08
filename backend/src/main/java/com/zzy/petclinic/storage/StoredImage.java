package com.zzy.petclinic.storage;

import java.io.IOException;
import java.io.InputStream;

public record StoredImage(InputStream inputStream, String contentType, long contentLength, String etag)
    implements AutoCloseable {
  @Override
  public void close() throws IOException {
    inputStream.close();
  }
}
