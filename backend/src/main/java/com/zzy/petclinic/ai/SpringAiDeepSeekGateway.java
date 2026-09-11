package com.zzy.petclinic.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzy.petclinic.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

/**
 * 通过 Spring AI 的 OpenAI 兼容客户端调用 DeepSeek。
 *
 * <p>该类负责把模型与工具调用中的技术异常转换为稳定的 HTTP 业务语义，并严格将最终 JSON
 * 解析为 {@link AiContracts.ModelReply}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiDeepSeekGateway implements AiModelGateway{

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvide;
    private final ObjectMapper objectMapper;

    /**
     * 调用模型、执行模型选中的工具，并解析最终结构化回复。
     *
     * @param messages 完整的本次提示消息
     * @param tools 允许 Spring AI 暴露给模型的工具对象
     * @return 通过严格 JSON 反序列化得到的回复
     * @throws BusinessException AI 未启用、超时、工具执行失败或返回内容不合法时抛出
     */
    @Override
    /*
     * 不恰当：这里与接口一样把 Object 误写为了 Objects，导致 AiTools 无法传入。
     * public AiContracts.ModelReply generate(List<Message> messages, Objects... tools) {
     */
    public AiContracts.ModelReply generate(List<Message> messages, Object... tools) {
        ChatClient.Builder builder;
        try {
            /*
             * 不恰当：当 chat=none 时，Spring 仍可能保留一个延迟创建的 ChatClient.Builder
             * BeanDefinition。直接 getIfAvailable() 会触发创建，然后因缺少 ChatModel
             * 抛出 UnsatisfiedDependencyException，而不是简单返回 null。
             * ChatClient.Builder builder = chatClientBuilderProvide.getIfAvailable();
             */
            builder = chatClientBuilderProvide.getIfAvailable();
        } catch (BeansException exception) {
            log.info("AI ChatClient is unavailable: {}", exception.getClass().getSimpleName());
            throw aiDisabled();
        }
        if(builder==null){
            throw aiDisabled();
        }
        String raw;
        try{
            raw=builder.build()
                    .prompt(new Prompt(messages))
                    .tools(tools)
                    .call()
                    .content();
        }catch (ResourceAccessException | TransientAiException exception){
            log.warn(
                    "DeepSeek request unavailable: {}",
                    exception.getClass().getSimpleName()
            );
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"ai连接超时或暂时不可用，请稍后重试");
        } catch (NonTransientAiException exception) {
            log.error(
                    "DeepSeek request rejected: {}",
                    exception.getClass().getSimpleName()
            );
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"ai服务配置错误或请求被供应商拒绝");
        } catch (ToolExecutionException exception) {
            log.error(
                    "AI tool execution failed: {}",
                    exception.getClass().getSimpleName());

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 工具执行失败，请稍后重试");

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected AI model error: {}",
                    exception.getClass().getSimpleName());

            throw new BusinessException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 服务返回异常，请稍后重试");
        }

        if(!StringUtils.hasText(raw)){
            throw new BusinessException(HttpStatus.BAD_GATEWAY,"ai返回了空内容，请重试");
        }

        try{
            ObjectMapper strictMapper = objectMapper.copy();
            strictMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            strictMapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            return strictMapper.readValue(raw,AiContracts.ModelReply.class);
        }catch (JsonProcessingException exception){
            log.warn("DeepSeek returned malformed structured output");
            throw new BusinessException(HttpStatus.BAD_GATEWAY,"AI 返回的结构化数据格式不正确，请重试");
        }
    }

    /**
     * 创建 AI 聊天未启用时的统一 503 异常。
     *
     * @return 可由全局异常处理器转成 503 响应的业务异常
     */
    private BusinessException aiDisabled() {
        return new BusinessException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI服务未启用，请配置DEEPSEEK_API_KEY，并设置AI_CHAT_MODEL=openai");
    }
}
