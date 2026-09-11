package com.zzy.petclinic.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool调用记录器
 * 记录当前一次请求中，ai调用了哪些工具
 */
@Component
public class AiToolCallRecorder {
    private final ThreadLocal<List<ToolUse>> holder = ThreadLocal.withInitial(ArrayList::new);

    public void begin() {
        holder.set(new ArrayList<>());
    }

    public void record(
            String name,
            boolean success,
            long durationMs,
            int resultCount
    ) {
        holder.get().add(new ToolUse(name, success, durationMs, resultCount));
    }

    public List<ToolUse> drain(){
        List<ToolUse> result =List.copyOf(holder.get());
        holder.remove();
        return result;
    }

    public void clear(){holder.remove();}

    /**
     * 不保存工具参数和完整查询结果，避免将宠物过敏史等内容
     * 重复写入应用日志和审计字段。
     */
    public record ToolUse(
            String name,
            boolean success,
            long durationMs,
            int resultCount
    ){}
}
