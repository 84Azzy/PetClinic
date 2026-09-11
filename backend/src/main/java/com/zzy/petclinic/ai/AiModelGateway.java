package com.zzy.petclinic.ai;

import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.Objects;

/**
 *接口作用
 * - 业务 Service 不直接依赖 DeepSeek HTTP 细节；
 * - 单元测试可以 Mock `AiModelGateway`；
 * - 自动化测试不会消耗真实 Token；
 * - 非法 JSON 和超时集中转换成明确业务错误。
 */
public interface AiModelGateway {
    //todo AiContracts.ModelReply在这是咋用的，generate里面的参数和AiContracts.ModelReply的参数如何联动
    AiContracts.ModelReply generate(
            List<Message>messages,
            Objects... tools
    );
}
