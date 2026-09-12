package com.zzy.petclinic.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录当前一次 AI 请求中执行过的工具及其概要结果。
 *
 * <p>记录按请求线程放在 {@link ThreadLocal} 中，避免并发请求互相混入。一轮调用结束后必须通过
 * {@link #drain()} 取走记录，或在异常时通过 {@link #clear()} 清理，防止线程池复用线程时残留数据。
 */
@Component
public class AiToolCallRecorder {
    private final ThreadLocal<List<ToolUse>> holder = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 开始记录新的一轮 AI 请求，并丢弃当前线程上可能残留的旧记录。
     */
    public void begin() {
        holder.set(new ArrayList<>());
    }

    /**
     * TODO 在AiTools的query方法中被调用，tool执行完只读查询后，无论是否有报错，都调用了record()进行记录
     * 向当前请求追加一条工具执行概要。
     *
     * @param name 工具名称
     * @param success 工具是否成功返回
     * @param durationMs 执行耗时，单位毫秒
     * @param resultCount 返回的记录数；失败时为 0
     */
    public void record(
            String name,
            boolean success,
            long durationMs,
            int resultCount
    ) {
        holder.get().add(new ToolUse(name, success, durationMs, resultCount));
    }

    /**
     * 返回当前请求的不可变记录快照，并立即清理 {@link ThreadLocal}。
     *
     * @return 按执行顺序排列的工具调用记录
     */
    public List<ToolUse> drain(){
        List<ToolUse> result =List.copyOf(holder.get());
        holder.remove();
        return result;
    }

    /**
     * 在调用异常结束时清理当前线程的记录。
     */
    public void clear(){holder.remove();}

    /**
     * 单次工具调用的最小审计信息。
     *
     * <p>不保存工具参数和完整查询结果，避免将宠物过敏史等内容重复写入应用日志和审计字段。
     *
     * @param name 工具名称
     * @param success 是否成功
     * @param durationMs 耗时，单位毫秒
     * @param resultCount 结果数量
     */
    public record ToolUse(
            String name,
            boolean success,
            long durationMs,
            int resultCount
    ){}
}
