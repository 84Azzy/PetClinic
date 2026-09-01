package com.zzy.petclinic.visit;

import jakarta.validation.constraints.*;

/**
 * 创建预约时的请求参数。
 *
 * <p>请求中不接收 {@code vetId} 和 {@code createdBy}：兽医编号必须从所选时段中取得，创建人必须从当前登录用户中取得，
 * 不能相信客户端传来的身份信息。
 *
 * @param petId 要就诊的宠物编号；Service 还需校验宠物存在、处于 ACTIVE 状态且属于当前用户
 * @param slotId 要抢占的排班时段编号；Service 通过条件更新保证同一时段只能预约一次
 * @param requestId 客户端为本次操作生成的幂等键；重试同一请求时不得重复创建预约
 * @param reason 就诊原因；不能为空，最长 500 个字符
 */
public record VisitRequest(
    @NotNull Long petId,
    @NotNull Long slotId,
    @NotBlank @Size(max = 64) String requestId,
    @NotBlank @Size(max = 500) String reason) {}
