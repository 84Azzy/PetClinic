package com.zzy.petclinic.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** 验证工具记录只在当前一轮请求中存活。 */
class AiToolCallRecorderTest {

  @Test
  void drainReturnsSnapshotAndClearsCurrentThread() {
    AiToolCallRecorder recorder = new AiToolCallRecorder();
    recorder.begin();
    recorder.record("listMyPets", true, 3L, 2);

    assertEquals(
        List.of(new AiToolCallRecorder.ToolUse("listMyPets", true, 3L, 2)),
        recorder.drain());
    assertEquals(List.of(), recorder.drain());
  }

  @Test
  void beginDiscardsAnUnfinishedPreviousRound() {
    AiToolCallRecorder recorder = new AiToolCallRecorder();
    recorder.record("old", false, 1L, 0);

    recorder.begin();
    recorder.record("new", true, 2L, 1);

    assertEquals(List.of("new"), recorder.drain().stream().map(AiToolCallRecorder.ToolUse::name).toList());
  }
}
