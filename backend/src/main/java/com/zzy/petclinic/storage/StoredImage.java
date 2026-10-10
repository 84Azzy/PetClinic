package com.zzy.petclinic.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 从对象存储读取到的图片流及其响应元数据。
 *
 * <p>{@code inputStream} 通常直接连接 COS 的 HTTP 响应，因此必须及时关闭。本记录实现 {@link AutoCloseable}，
 * 方便 {@link ImageResponses} 在流式传输结束后释放网络连接。
 *
 * @param inputStream 图片内容流
 * @param contentType COS 对象的 MIME 类型
 * @param contentLength 图片字节数；未知时可以为负数
 * @param etag COS 返回的对象 ETag，可用于浏览器缓存校验
 */
public record StoredImage(InputStream inputStream, String contentType, long contentLength, String etag)
    implements AutoCloseable {
  @Override
  public void close() throws IOException {
    inputStream.close();
  }
}
