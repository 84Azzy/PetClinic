package com.zzy.petclinic.storage;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** 把对象存储中的图片转换为浏览器可消费的 HTTP 二进制响应。 */
public final class ImageResponses {
  private ImageResponses() {}

  /**
   * 将 COS 输入流直接复制到响应输出流。
   *
   * <p>这种代理读取方式使私有 COS 桶无需公开访问或配置浏览器 CORS；浏览器只需携带本系统 JWT 请求后端。
   * {@code Content-Type} 告诉浏览器图片格式，{@code Content-Length} 便于传输，{@code ETag} 可参与缓存校验。
   * 响应使用 private/no-cache：浏览器可以保存副本，但复用前需要重新校验，避免用户替换图片后长期看到旧内容。
   *
   * @param image 存储层打开的图片流；本方法会在传输结束后关闭它
   * @return Spring MVC 可异步写出的流式响应
   */
  public static ResponseEntity<StreamingResponseBody> stream(StoredImage image) {
    // 图片流直接复制到响应输出流
    StreamingResponseBody body =
        outputStream -> {
          try (image) {
            image.inputStream().transferTo(outputStream);
          }
        };
    // 构建响应实体
    ResponseEntity.BodyBuilder response =
        ResponseEntity.ok()
            .contentType(safeMediaType(image.contentType()))// 设置响应内容类型
            .cacheControl(CacheControl.noCache().cachePrivate());// 设置缓存控制为私有，不缓存
    if (image.contentLength() >= 0) response.contentLength(image.contentLength());
    if (image.etag() != null && !image.etag().isBlank()) response.eTag(image.etag());
    return response.body(body);
  }

  /** 防止异常的 COS 元数据把响应伪装成 HTML 等非图片内容。 */
  private static MediaType safeMediaType(String contentType) {
    try {
      MediaType mediaType = MediaType.parseMediaType(contentType);
      return "image".equals(mediaType.getType()) ? mediaType : MediaType.APPLICATION_OCTET_STREAM;
    } catch (RuntimeException ignored) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
  }
}
