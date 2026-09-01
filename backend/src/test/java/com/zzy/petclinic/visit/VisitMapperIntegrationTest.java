package com.zzy.petclinic.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

/** 使用 H2 执行 VisitMapper.xml，验证条件更新 SQL 的真实行为。 */
@SpringBootTest
@Sql(
    statements = {
      "DROP TABLE IF EXISTS visit",
      "DROP TABLE IF EXISTS vet_schedule_slot",
      "CREATE TABLE vet_schedule_slot (id BIGINT PRIMARY KEY, vet_id BIGINT NOT NULL,"
          + " start_time TIMESTAMP, end_time TIMESTAMP, status VARCHAR(20) NOT NULL,"
          + " note VARCHAR(255), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE visit (id BIGINT PRIMARY KEY, pet_id BIGINT NOT NULL, slot_id BIGINT NOT NULL,"
          + " vet_id BIGINT NOT NULL, created_by BIGINT NOT NULL, request_id VARCHAR(64) NOT NULL UNIQUE,"
          + " reason VARCHAR(500) NOT NULL, status VARCHAR(20) NOT NULL, cancelled_at TIMESTAMP,"
          + " cancel_reason VARCHAR(255), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "INSERT INTO vet_schedule_slot(id, vet_id, status, updated_at)"
          + " VALUES (20, 30, 'AVAILABLE', CURRENT_TIMESTAMP)",
      "INSERT INTO visit(id, pet_id, slot_id, vet_id, created_by, request_id, reason, status)"
          + " VALUES (1, 10, 20, 30, 3, 'req-1', '检查', 'SCHEDULED')"
    })
class VisitMapperIntegrationTest {
  @Autowired private VisitMapper visitMapper;

  @Test
  void sameSlotCanOnlyBeClaimedOnceEvenWhenTwoThreadsStartTogether() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Integer>> futures = new ArrayList<>();
      for (int i = 0; i < 2; i++) {
        futures.add(
            executor.submit(
                () -> {
                  start.await(5, TimeUnit.SECONDS);
                  return visitMapper.claimSlot(20L);
                }));
      }

      start.countDown();
      List<Integer> affectedRows =
          new ArrayList<>(List.of(futures.get(0).get(), futures.get(1).get()));
      Collections.sort(affectedRows);

      assertEquals(List.of(0, 1), affectedRows);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void releaseOnlyChangesABookedSlot() {
    assertEquals(0, visitMapper.releaseSlot(20L));
    assertEquals(1, visitMapper.claimSlot(20L));
    assertEquals(1, visitMapper.releaseSlot(20L));
    assertEquals(0, visitMapper.releaseSlot(20L));
  }

  @Test
  void customQueriesBindParametersAndMapSnakeCaseColumns() {
    Visit byRequestId = visitMapper.selectByRequestId("req-1");
    List<Visit> mine = visitMapper.selectMine(3L);

    assertEquals(1L, byRequestId.getId());
    assertEquals(3L, byRequestId.getCreatedBy());
    assertEquals(List.of(1L), mine.stream().map(Visit::getId).toList());
    assertNull(visitMapper.selectByRequestId("missing"));
  }
}
