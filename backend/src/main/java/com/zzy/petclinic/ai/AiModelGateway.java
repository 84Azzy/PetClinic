package com.zzy.petclinic.ai;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * AI 模型网关，用于隔离业务层与具体模型供应商。
 *
 * <p>业务 Service 只依赖该接口，不需要知道 DeepSeek HTTP 细节；测试时也可以直接 Mock
 * 该接口，避免消耗真实 Token。超时、供应商异常和非法 JSON 由具体实现集中转换为业务异常。
 */
public interface AiModelGateway {

    /**
     * 根据对话消息调用模型，并允许模型调用指定的工具对象。
     *
     * <p>原 TODO 解答：{@link AiContracts.ModelReply} 是方法的「输出类型」，{@code messages} 和
     * {@code tools} 是「输入」，它们之间没有 Java 层面的参数一一联动。具体实现先把消息和工具交给模型，
     * 再把模型返回的 JSON 反序列化为 {@code ModelReply(answer, draft)}。系统提示词负责约束 JSON
     * 外形，Jackson 负责将 JSON 字段映射到 record 组件。
     *
     * @param messages 发给模型的系统消息、历史消息和当前用户消息
     * @param tools 可供模型选择调用的 Spring AI 工具对象
     * @return 从模型最终 JSON 中解析得到的结构化回复
     */
    /*
     * 不恰当：Objects 是 java.util 中的工具类，表示参数只能是 Objects 实例，
     * AiTools 因此无法传入，编译会报 varargs 类型不匹配。
     * AiContracts.ModelReply generate(List<Message> messages, Objects... tools);
     */
    AiContracts.ModelReply generate(
            List<Message> messages,
            Object... tools
    );
}
