package com.zzy.petclinic.visit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import com.zzy.petclinic.pet.Pet;
import com.zzy.petclinic.pet.PetService;
import com.zzy.petclinic.schedule.ScheduleService;
import com.zzy.petclinic.schedule.VetScheduleSlot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;

/** 验证 VisitServiceImpl 的 Spring 事务会把“已抢占时段”与“预约插入”作为一个整体提交或回滚。 */
@SpringBootTest
@Sql(
    statements = {
      "DROP TABLE IF EXISTS visit",
      "DROP TABLE IF EXISTS vet_schedule_slot",
      "CREATE TABLE vet_schedule_slot (id BIGINT PRIMARY KEY, vet_id BIGINT NOT NULL,"
          + " start_time TIMESTAMP, end_time TIMESTAMP, status VARCHAR(20) NOT NULL,"
          + " note VARCHAR(255), created_at TIMESTAMP, updated_at TIMESTAMP)",
      "CREATE TABLE visit (id BIGINT AUTO_INCREMENT PRIMARY KEY, pet_id BIGINT NOT NULL,"
          + " slot_id BIGINT NOT NULL, vet_id BIGINT NOT NULL, created_by BIGINT NOT NULL,"
          + " request_id VARCHAR(64) NOT NULL UNIQUE, reason VARCHAR(500) NOT NULL,"
          + " status VARCHAR(20) NOT NULL, cancelled_at TIMESTAMP, cancel_reason VARCHAR(255),"
          + " created_at TIMESTAMP, updated_at TIMESTAMP, CHECK (reason <> 'FORCE_INSERT_FAILURE'))",
      "INSERT INTO vet_schedule_slot(id, vet_id, status, updated_at)"
          + " VALUES (20, 30, 'AVAILABLE', CURRENT_TIMESTAMP)"
    })
class VisitTransactionIntegrationTest {
  @Autowired private VisitServiceImpl service;
  @Autowired private JdbcTemplate jdbcTemplate;

  @MockBean private CurrentUser currentUser;
  @MockBean private OwnerService ownerService;
  @MockBean private PetService petService;
  @MockBean private ScheduleService scheduleService;

  @BeforeEach
  void setUpBusinessDependencies() {
    AuthenticatedUser principal = ownerUser(3L);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    when(currentUser.require()).thenReturn(principal);

    Owner owner = new Owner();
    owner.setId(1L);
    owner.setUserId(3L);
    when(ownerService.mine()).thenReturn(owner);

    Pet pet = new Pet();
    pet.setId(10L);
    pet.setOwnerId(1L);
    pet.setStatus("ACTIVE");
    when(petService.get(10L)).thenReturn(pet);

    VetScheduleSlot slot = new VetScheduleSlot();
    slot.setId(20L);
    slot.setVetId(30L);
    slot.setStatus("AVAILABLE");
    when(scheduleService.get(20L)).thenReturn(slot);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void successfulCreateCommitsBothVisitAndBookedSlot() {
    service.create(new VisitRequest(10L, 20L, "req-success", "常规检查"));

    assertEquals(1, countVisits());
    assertEquals("BOOKED", slotStatus());
  }

  @Test
  void insertFailureRollsTheClaimedSlotBackToAvailable() {
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            service.create(
                new VisitRequest(
                    10L, 20L, "req-failure", "FORCE_INSERT_FAILURE")));

    assertEquals(0, countVisits());
    assertEquals("AVAILABLE", slotStatus());
  }

  private Integer countVisits() {
    return jdbcTemplate.queryForObject("select count(*) from visit", Integer.class);
  }

  private String slotStatus() {
    return jdbcTemplate.queryForObject(
        "select status from vet_schedule_slot where id = 20", String.class);
  }

  private static AuthenticatedUser ownerUser(Long id) {
    SysUser user = new SysUser();
    user.setId(id);
    user.setUsername("owner-" + id);
    user.setPasswordHash("password");
    user.setAccountType("OWNER");
    user.setStatus("ACTIVE");
    return new AuthenticatedUser(
        user, new UserAuthorities(java.util.Set.of("OWNER"), java.util.Set.of("visit:create")));
  }
}
