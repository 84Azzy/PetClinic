package com.zzy.petclinic.storage;

import com.zzy.petclinic.common.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ImageUploadValidator {
  private static final long MAX_PIXELS = 25_000_000L;
  private final ImageStorageProperties properties;

  public ValidatedImage validate(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw BusinessException.badRequest("请选择要上传的图片");
    }
    if (file.getSize() > properties.getMaxSize().toBytes()) {
      throw BusinessException.badRequest("图片大小不能超过 " + properties.getMaxSize());
    }

    try {
      byte[] bytes = file.getBytes();
      ImageType type = detect(bytes);
      if (!properties.getAllowedContentTypes().contains(type.contentType())) {
        throw BusinessException.badRequest("仅支持 JPEG、PNG 或 WebP 图片");
      }
      validateDimensions(bytes, type);
      return new ValidatedImage(bytes, type.contentType(), type.extension());
    } catch (IOException e) {
      throw BusinessException.badRequest("图片读取失败，请重新选择文件");
    }
  }

  private ImageType detect(byte[] bytes) {
    if (bytes.length >= 3
        && (bytes[0] & 0xff) == 0xff
        && (bytes[1] & 0xff) == 0xd8
        && (bytes[2] & 0xff) == 0xff) {
      return new ImageType("image/jpeg", "jpg");
    }
    if (bytes.length >= 8
        && (bytes[0] & 0xff) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4e
        && bytes[3] == 0x47
        && bytes[4] == 0x0d
        && bytes[5] == 0x0a
        && bytes[6] == 0x1a
        && bytes[7] == 0x0a) {
      return new ImageType("image/png", "png");
    }
    if (bytes.length >= 12
        && ascii(bytes, 0, 4).equals("RIFF")
        && ascii(bytes, 8, 4).equals("WEBP")) {
      return new ImageType("image/webp", "webp");
    }
    throw BusinessException.badRequest("文件内容不是受支持的图片格式");
  }

  private void validateDimensions(byte[] bytes, ImageType type) throws IOException {
    Dimensions dimensions;
    if (type.contentType().equals("image/webp")) {
      dimensions = webpDimensions(bytes);
    } else {
      BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
      if (image == null) throw BusinessException.badRequest("图片内容已损坏或无法解析");
      dimensions = new Dimensions(image.getWidth(), image.getHeight());
    }
    long pixels = (long) dimensions.width() * dimensions.height();
    if (dimensions.width() <= 0 || dimensions.height() <= 0 || pixels > MAX_PIXELS) {
      throw BusinessException.badRequest("图片尺寸过大，像素总数不能超过 2500 万");
    }
  }

  private Dimensions webpDimensions(byte[] bytes) {
    if (bytes.length < 30) throw BusinessException.badRequest("WebP 图片内容已损坏");
    String chunk = ascii(bytes, 12, 4);
    if (chunk.equals("VP8X")) {
      return new Dimensions(1 + littleEndian24(bytes, 24), 1 + littleEndian24(bytes, 27));
    }
    if (chunk.equals("VP8L") && (bytes[20] & 0xff) == 0x2f) {
      int b1 = bytes[21] & 0xff;
      int b2 = bytes[22] & 0xff;
      int b3 = bytes[23] & 0xff;
      int b4 = bytes[24] & 0xff;
      int width = 1 + (b1 | ((b2 & 0x3f) << 8));
      int height = 1 + ((b2 >> 6) | (b3 << 2) | ((b4 & 0x0f) << 10));
      return new Dimensions(width, height);
    }
    if (chunk.equals("VP8 ")
        && (bytes[23] & 0xff) == 0x9d
        && (bytes[24] & 0xff) == 0x01
        && (bytes[25] & 0xff) == 0x2a) {
      int width = ((bytes[26] & 0xff) | ((bytes[27] & 0xff) << 8)) & 0x3fff;
      int height = ((bytes[28] & 0xff) | ((bytes[29] & 0xff) << 8)) & 0x3fff;
      return new Dimensions(width, height);
    }
    throw BusinessException.badRequest("WebP 图片内容已损坏或无法解析");
  }

  private int littleEndian24(byte[] bytes, int offset) {
    return (bytes[offset] & 0xff)
        | ((bytes[offset + 1] & 0xff) << 8)
        | ((bytes[offset + 2] & 0xff) << 16);
  }

  private String ascii(byte[] bytes, int offset, int length) {
    return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII)
        .toUpperCase(Locale.ROOT);
  }

  private record ImageType(String contentType, String extension) {}

  private record Dimensions(int width, int height) {}
}
