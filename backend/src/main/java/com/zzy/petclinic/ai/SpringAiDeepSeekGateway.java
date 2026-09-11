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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiDeepSeekGateway implements AiModelGateway{

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvide;
    private final ObjectMapper objectMapper;

    @Override
    public AiContracts.ModelReply generate(List<Message> messages, Objects... tools) {
        ChatClient.Builder builder = chatClientBuilderProvide.getIfAvailable();
        if(builder==null){
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"AI服务未启用，请配置DEEPSEEK_API_KEY，并设置AI_CHAT_MODEL=openai");
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
}
