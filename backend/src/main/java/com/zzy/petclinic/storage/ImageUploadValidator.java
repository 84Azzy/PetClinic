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

/**
 * 在图片进入 COS 之前执行服务端校验。
 *
 * <p>不能相信浏览器提交的文件名和 {@code Content-Type}：攻击者可以绕过前端校验并伪造请求。因此这里根据
 * 文件魔数识别 JPEG、PNG、WebP，并限制文件字节数和解码后的像素总数。像素限制用于防御“压缩后很小、解码后
 * 占用巨大内存”的图片炸弹。
 */
@Component
@RequiredArgsConstructor
public class ImageUploadValidator {
  private static final long MAX_PIXELS = 25_000_000L;
  private final ImageStorageProperties properties;

  /**
   * 读取并验证 Multipart 文件，成功后生成可信的 {@link ValidatedImage}。
   *
   * @param file Controller 从 {@code multipart/form-data} 的 {@code file} 字段取得的文件
   * @return 可安全交给存储层的图片字节、MIME 类型和扩展名
   */
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

  /** 根据文件头（魔数）识别真实图片格式，不采用用户可伪造的文件扩展名。 */
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

  /** 解码尺寸并拒绝无效或像素数过大的图片。 */
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

  /**
   * 从 WebP 的 VP8X、VP8L 或 VP8 数据块读取宽高。
   *
   * <p>标准 JDK 的 ImageIO 通常不原生支持 WebP，因此这里解析格式头，而不是依赖额外图像插件。
   */
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

  // 图片类型记录
  private record ImageType(String contentType, String extension) {}

  // 图片尺寸记录
  private record Dimensions(int width, int height) {}
}
