package com.zzy.petclinic.storage;

/**
 * 通过 {@link ImageUploadValidator} 校验后的图片值对象。
 *
 * @param bytes 要上传到 COS 的原始字节
 * @param contentType 根据文件内容识别出的 MIME 类型，而非客户端声明值
 * @param extension 与真实格式匹配的安全扩展名
 */
public record ValidatedImage(byte[] bytes, String contentType, String extension) {}
