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
@Slf4j      //加了它后可直接使用log.warn等日志打印
@Component
@RequiredArgsConstructor
public class SpringAiDeepSeekGateway implements AiModelGateway{

    /**
     * 使用ObjectProvider包裹需要的bean后，可以实现延迟创建
     *
     * 实现原理：
     * 构造器注入时不解析真正的bean,而是注入一个以后查询bean的句柄，把解析动作推迟到调用getIfAvailable()
     */
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    //jackson里面的json转换器，json->java
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
             * AI_CHAT_MODEL 是环境变量，它通过 application.yml 中的
             * spring.ai.model.chat: ${AI_CHAT_MODEL:none}
             * 映射为 Spring AI 的模型选择配置。Spring Boot 启动时，Spring AI
             * 根据该配置决定是否创建 ChatModel，以及创建哪种 ChatModel：
             *
             * 1. AI_CHAT_MODEL=openai：
             *    创建基于 OpenAI 兼容协议的 ChatModel。本项目又通过
             *    spring.ai.openai.base-url 指向 DeepSeek，所以实际调用的是 DeepSeek。
             *
             * 2. AI_CHAT_MODEL=none：
             *    不创建可用的 ChatModel。这里的“允许关闭 AI”是指应用可以在没有
             *    ChatModel 的情况下正常启动，普通业务不会因为 AI 未配置而启动失败。
             *
             * ChatClient.Builder 和 ChatModel 存在依赖关系，可以简化理解为：
             *
             * ChatClient.Builder chatClientBuilder(ChatModel chatModel) {
             *     return ChatClient.builder(chatModel);
             * }
             *
             * 因此，创建 ChatClient.Builder 时必须先找到可用的 ChatModel。
             *
             * 这里注入 ObjectProvider<ChatClient.Builder>，而不是直接注入
             * ChatClient.Builder。ObjectProvider 中保存的不是已经创建好的 Builder，
             * 而是一个以后向 Spring BeanFactory 查询 Builder 的句柄。这样在创建
             * SpringAiDeepSeekGateway 时，不会因为构造器依赖而立即解析 Builder，
             * 从而允许项目在 AI 关闭时正常启动。
             *
             * getIfAvailable() 本身不会读取 AI_CHAT_MODEL。AI_CHAT_MODEL 已经在
             * Spring 启动和自动配置阶段影响了 ChatModel 以及相关 Bean 的注册状态。
             * 调用 getIfAvailable() 时，它只会向 Spring 容器查询
             * ChatClient.Builder；如果 Builder 尚未创建，Spring 会尝试创建它，
             * 并在创建过程中继续解析它依赖的 ChatModel。
             *
             * 调用结果可能有三种：
             *TODO BeanDefinition只是一张登记如何创建builder的说明书，不是真正的bean
             *
             * 1. 容器中不存在 ChatClient.Builder 的候选 BeanDefinition：
             *    getIfAvailable() 返回 null。
             *
             * 2. Builder BeanDefinition 存在，而且所依赖的 ChatModel 也可用：
             *    Spring 成功创建并返回 ChatClient.Builder。
             *
             * 3. Builder BeanDefinition 存在，但 AI_CHAT_MODEL=none 导致没有
             *    可用的 ChatModel：
             *    Spring 尝试创建 Builder 时依赖解析失败，抛出 BeansException，
             *    而不是返回 null。
             *
             * 因此，这里既要捕获 Bean 创建失败产生的 BeansException，也要在
             * getIfAvailable() 返回后判断 builder 是否为 null。两种情况最终都
             * 被转换成统一的“AI 服务未启用”503 业务异常。
             *
             * 依赖关系可以概括为：
             *
             * AI_CHAT_MODEL
             *     -> spring.ai.model.chat
             *     -> Spring AI 是否创建 ChatModel
             *     -> ChatClient.Builder 能否成功创建
             *     -> getIfAvailable() 返回 Builder、null 或抛出 BeansException
             */
            builder = chatClientBuilderProvider.getIfAvailable();
        } catch (BeansException exception) {
            //捕获的情况为没有BeanDefinition,即没有builder定义
            log.info("AI ChatClient is unavailable: {}", exception.getClass().getSimpleName());
            throw aiDisabled();
        }
        //有BeanDefinition，但配置的chatModel=none
        if(builder==null){
            throw aiDisabled();
        }
        //builder及依赖chatModel正常的情况


        /*
        *设置一个只供当前使用的mapper
        * 因为传入的objectMapper是整个springBoot共享的
        * 如果直接objectMapper.enable(...)会影响普通接口
        * FAIL_ON_UNKNOWN_PROPERTIES模型多返回字段时拒绝解析。
        * FAIL_ON_TRAILING_TOKENS一个 JSON 对象结束后不能再出现其他 JSON 内容。
         */
        ObjectMapper strictMapper = objectMapper.copy();
        strictMapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);//拒绝一个json内部有多余字段
        strictMapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);//拒绝多个json

        //对“空输出或错误 JSON”重试一次，而不是对所有错误重试一次。
        for(int attempt=1;attempt<=2;attempt++){
            String raw = callOnce(builder,messages,tools);
            if(!StringUtils.hasText(raw)){
                log.warn("DeepSeek returned empty content, attempt={}",attempt);
            }else{
                try{
                    return strictMapper.readValue(raw,AiContracts.ModelReply.class);
                }catch (JsonProcessingException exception){
                    log.warn("DeepSeek returned malformed structured output, attempt={}",attempt);
                }
            }
            if(attempt==1)log.info("Retrying DeepSeek once after invalid output");
        }
        throw new BusinessException(
                HttpStatus.BAD_GATEWAY,
                "AI连续返回空内容或错误格式，请稍后重试");
    }

    private String callOnce(ChatClient.Builder builder,List<Message> messages,Object... tools){
        try{
            return builder.build()
                    .prompt(new Prompt(messages))
                    .tools(tools)
                    .call() //同步、阻塞式调用模型。
                    .content(); //取最终助手消息的文本内容。
        }catch (ResourceAccessException | TransientAiException exception){
            log.warn("DeepSeek request unavailable: {}",exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"ai连接超时或暂时不可用，请稍后重试");
        }catch (NonTransientAiException exception){
            log.error("DeepSeek request rejected: {}",exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"ai服务配置错误或请求被供应商拒绝");
        }catch (ToolExecutionException exception){
            log.error("AI tool execution failed: {}",exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,"AI 工具执行失败，请稍后重试");
        }catch (RuntimeException exception){
            log.error("Unexpected AI model error: {}",exception.getClass().getSimpleName());
            throw new BusinessException(HttpStatus.BAD_GATEWAY,"AI 服务返回异常，请稍后重试");
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
