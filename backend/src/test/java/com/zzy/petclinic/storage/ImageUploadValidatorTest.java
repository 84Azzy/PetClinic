package com.zzy.petclinic.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zzy.petclinic.common.BusinessException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class ImageUploadValidatorTest {
  private final ImageStorageProperties properties = new ImageStorageProperties();
  private final ImageUploadValidator validator = new ImageUploadValidator(properties);

  @Test
  void detectsImageFromContentInsteadOfTrustingFilename() {
    byte[] png =
        Base64.getDecoder()
            .decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    MockMultipartFile file =
        new MockMultipartFile("file", "not-an-image.txt", "text/plain", png);

    ValidatedImage image = validator.validate(file);

    assertEquals("image/png", image.contentType());
    assertEquals("png", image.extension());
  }

  @Test
  void rejectsUnsupportedContent() {
    MockMultipartFile file =
        new MockMultipartFile("file", "fake.jpg", "image/jpeg", "not an image".getBytes());

    assertThrows(BusinessException.class, () -> validator.validate(file));
  }

  @Test
  void rejectsFilesOverConfiguredLimit() {
    properties.setMaxSize(DataSize.ofBytes(4));
    MockMultipartFile file =
        new MockMultipartFile("file", "large.png", "image/png", new byte[5]);

    assertThrows(BusinessException.class, () -> validator.validate(file));
  }

  @Test
  void acceptsWebpWithValidVp8xDimensions() {
    byte[] webp = new byte[30];
    System.arraycopy("RIFF".getBytes(), 0, webp, 0, 4);
    System.arraycopy("WEBP".getBytes(), 0, webp, 8, 4);
    System.arraycopy("VP8X".getBytes(), 0, webp, 12, 4);
    MockMultipartFile file = new MockMultipartFile("file", "pet.webp", "image/webp", webp);

    ValidatedImage image = validator.validate(file);

    assertEquals("image/webp", image.contentType());
    assertEquals("webp", image.extension());
  }

  @Test
  void rejectsWebpOverPixelLimit() {
    byte[] webp = new byte[30];
    System.arraycopy("RIFF".getBytes(), 0, webp, 0, 4);
    System.arraycopy("WEBP".getBytes(), 0, webp, 8, 4);
    System.arraycopy("VP8X".getBytes(), 0, webp, 12, 4);
    webp[24] = (byte) 0xff;
    webp[25] = (byte) 0xff;
    webp[27] = (byte) 0xff;
    webp[28] = (byte) 0xff;
    MockMultipartFile file = new MockMultipartFile("file", "huge.webp", "image/webp", webp);

    assertThrows(BusinessException.class, () -> validator.validate(file));
  }
}
