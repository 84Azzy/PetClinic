package com.zzy.petclinic.visit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 取消预约时的请求参数。
 *
 * @param reason 取消原因；不能为空，最长 255 个字符，与 {@code visit.cancel_reason} 字段长度一致
 */
public record CancelVisitRequest(@NotBlank @Size(max = 255) String reason) {}
