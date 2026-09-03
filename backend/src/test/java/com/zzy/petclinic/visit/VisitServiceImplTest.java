package com.zzy.petclinic.visit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import com.zzy.petclinic.pet.Pet;
import com.zzy.petclinic.pet.PetService;
import com.zzy.petclinic.schedule.ScheduleService;
import com.zzy.petclinic.schedule.VetScheduleSlot;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

/** VisitServiceImpl 的业务验收测试，重点保护幂等、归属、状态流转和冲突处理。 */
class VisitServiceImplTest {
  private VisitMapper visitMapper;
  private CurrentUser currentUser;
  private OwnerService ownerService;
  private PetService petService;
  private ScheduleService scheduleService;
  private VisitServiceImpl service;

  @BeforeEach
  void setUp() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), "visit-test"), Visit.class);
    visitMapper = mock(VisitMapper.class);
    currentUser = mock(CurrentUser.class);
    ownerService = mock(OwnerService.class);
    petService = mock(PetService.class);
    scheduleService = mock(ScheduleService.class);
    service =
        new VisitServiceImpl(
            visitMapper, currentUser, ownerService, petService, scheduleService);
  }

  @Test
  void createKeepsTheExistingAssemblyFlowAndBuildsScheduledVisit() {
    VisitRequest request = new VisitRequest(10L, 20L, "req-1", "常规检查");
    mockOwnerContext(3L, 1L);
    Pet pet = activePet(10L, 1L);
    VetScheduleSlot slot = slot(20L, 30L);
    when(petService.get(10L)).thenReturn(pet);
    when(scheduleService.get(20L)).thenReturn(slot);
    when(visitMapper.claimSlot(20L)).thenReturn(1);
    when(visitMapper.insert(any(Visit.class))).thenReturn(1);

    Visit created = service.create(request);

    assertAll(
        () -> assertEquals(10L, created.getPetId()),
        () -> assertEquals(20L, created.getSlotId()),
        () -> assertEquals(30L, created.getVetId()),
        () -> assertEquals(3L, created.getCreatedBy()),
        () -> assertEquals("SCHEDULED", created.getStatus()),
        () -> assertNotNull(created.getCreatedAt()),
        () -> assertNotNull(created.getUpdatedAt()));
    verify(visitMapper).claimSlot(20L);
    verify(visitMapper).insert(created);
  }

  @Test
  void sameRequestIdAndSamePayloadReturnsOriginalVisit() {
    VisitRequest request = new VisitRequest(10L, 20L, "req-1", "常规检查");
    Visit existing = scheduledVisit(1L, 3L, 10L, 20L);
    existing.setRequestId("req-1");
    existing.setReason("常规检查");
    when(currentUser.require()).thenReturn(user(3L, "OWNER"));
    when(visitMapper.selectByRequestId("req-1")).thenReturn(existing);

    assertSame(existing, service.create(request));
    verify(visitMapper, never()).claimSlot(any());
    verify(visitMapper, never()).insert(any(Visit.class));
  }

  @Test
  void sameRequestIdWithDifferentPayloadReturns409() {
    Visit existing = scheduledVisit(1L, 3L, 10L, 20L);
    existing.setRequestId("req-1");
    existing.setReason("原原因");
    when(currentUser.require()).thenReturn(user(3L, "OWNER"));
    when(visitMapper.selectByRequestId("req-1")).thenReturn(existing);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.create(new VisitRequest(10L, 20L, "req-1", "新原因")));

    assertEquals(409, exception.getStatus().value());
    verify(visitMapper, never()).claimSlot(any());
  }

  @Test
  void unavailableSlotReturns409BeforeInsert() {
    mockOwnerContext(3L, 1L);
    when(petService.get(10L)).thenReturn(activePet(10L, 1L));
    when(scheduleService.get(20L)).thenReturn(slot(20L, 30L));
    when(visitMapper.claimSlot(20L)).thenReturn(0);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.create(new VisitRequest(10L, 20L, "req-1", "检查")));

    assertEquals(409, exception.getStatus().value());
    verify(visitMapper, never()).insert(any(Visit.class));
  }

  @Test
  void duplicateDatabaseKeyIsConvertedTo409() {
    mockOwnerContext(3L, 1L);
    when(petService.get(10L)).thenReturn(activePet(10L, 1L));
    when(scheduleService.get(20L)).thenReturn(slot(20L, 30L));
    when(visitMapper.claimSlot(20L)).thenReturn(1);
    when(visitMapper.insert(any(Visit.class)))
        .thenThrow(new DuplicateKeyException("duplicate"));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.create(new VisitRequest(10L, 20L, "req-1", "检查")));

    assertEquals(409, exception.getStatus().value());
  }

  @Test
  void ownerPageAppliesOwnershipKeywordStatusAndOptionalFilters() {
    when(currentUser.require()).thenReturn(user(3L, "OWNER"));
    when(visitMapper.selectPage(any(Page.class), any(Wrapper.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.page(new PageQuery(1L, 10L, "检查", "SCHEDULED"), 10L, 30L);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Wrapper<Visit>> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(visitMapper).selectPage(any(Page.class), captor.capture());
    LambdaQueryWrapper<Visit> wrapper = (LambdaQueryWrapper<Visit>) captor.getValue();
    assertAll(
        () -> assertTrue(wrapper.getSqlSegment().contains("created_by")),
        () -> assertTrue(wrapper.getSqlSegment().contains("reason")),
        () -> assertTrue(wrapper.getSqlSegment().contains("request_id")),
        () -> assertTrue(wrapper.getSqlSegment().contains("status")),
        () -> assertTrue(wrapper.getParamNameValuePairs().containsValue(3L)),
        () -> assertTrue(wrapper.getParamNameValuePairs().containsValue(10L)),
        () -> assertTrue(wrapper.getParamNameValuePairs().containsValue(30L)),
        () -> assertTrue(wrapper.getParamNameValuePairs().containsValue("SCHEDULED")));
  }

  @Test
  void cancelPersistsCancelledStateAndReleasesBookedSlot() {
    Visit visit = scheduledVisit(1L, 3L, 10L, 20L);
    when(visitMapper.selectById(1L)).thenReturn(visit);
    when(currentUser.require()).thenReturn(user(3L, "OWNER"));
    when(visitMapper.updateById(visit)).thenReturn(1);
    when(visitMapper.releaseSlot(20L)).thenReturn(1);

    Visit cancelled = service.cancel(1L, new CancelVisitRequest("行程有变"));

    assertAll(
        () -> assertEquals("CANCELLED", cancelled.getStatus()),
        () -> assertEquals("行程有变", cancelled.getCancelReason()),
        () -> assertNotNull(cancelled.getCancelledAt()),
        () -> assertNotNull(cancelled.getUpdatedAt()));
    verify(visitMapper).updateById(visit);
    verify(visitMapper).releaseSlot(20L);
  }

  @Test
  void repeatedCancelReturns409WithoutReleasingAgain() {
    Visit visit = scheduledVisit(1L, 3L, 10L, 20L);
    visit.setStatus("CANCELLED");
    when(visitMapper.selectById(1L)).thenReturn(visit);
    when(currentUser.require()).thenReturn(user(3L, "OWNER"));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.cancel(1L, new CancelVisitRequest("再次取消")));

    assertEquals(409, exception.getStatus().value());
    verify(visitMapper, never()).updateById(any(Visit.class));
    verify(visitMapper, never()).releaseSlot(any());
  }

  @Test
  void ownerCannotCancelAnotherUsersVisit() {
    Visit visit = scheduledVisit(1L, 4L, 10L, 20L);
    when(visitMapper.selectById(1L)).thenReturn(visit);
    when(currentUser.require()).thenReturn(user(3L, "OWNER"));

    assertThrows(
        AccessDeniedException.class,
        () -> service.cancel(1L, new CancelVisitRequest("越权取消")));
    verify(visitMapper, never()).updateById(any(Visit.class));
    verify(visitMapper, never()).releaseSlot(any());
  }

  @Test
  void scheduledVisitCanBeCompletedWithoutReleasingTheSlot() {
    Visit visit = scheduledVisit(1L, 3L, 10L, 20L);
    when(visitMapper.selectById(1L)).thenReturn(visit);
    when(visitMapper.updateById(visit)).thenReturn(1);

    Visit completed = service.complete(1L);

    assertEquals("COMPLETED", completed.getStatus());
    verify(visitMapper).updateById(visit);
    verify(visitMapper, never()).releaseSlot(any());
  }

  @Test
  void controllerKeepsTheUsersDelegationCodeAndCreatedSemantics() {
    VisitController controller = new VisitController();
    VisitService visitService = mock(VisitService.class);
    ReflectionTestUtils.setField(controller, "visitService", visitService);
    VisitRequest request = new VisitRequest(10L, 20L, "req-1", "检查");
    Visit expected = new Visit();
    when(visitService.create(request)).thenReturn(expected);

    var response = controller.create(request);

    assertAll(
        () -> assertEquals(201, response.code()),
        () -> assertSame(expected, response.data()));
    verify(visitService).create(request);
  }

  private void mockOwnerContext(Long userId, Long ownerId) {
    when(currentUser.require()).thenReturn(user(userId, "OWNER"));
    Owner owner = new Owner();
    owner.setId(ownerId);
    owner.setUserId(userId);
    when(ownerService.mine()).thenReturn(owner);
  }

  private static AuthenticatedUser user(Long id, String accountType) {
    SysUser user = new SysUser();
    user.setId(id);
    user.setUsername("user-" + id);
    user.setPasswordHash("password");
    user.setAccountType(accountType);
    user.setStatus("ACTIVE");
    return new AuthenticatedUser(user, UserAuthorities.empty());
  }

  private static Pet activePet(Long id, Long ownerId) {
    Pet pet = new Pet();
    pet.setId(id);
    pet.setOwnerId(ownerId);
    pet.setStatus("ACTIVE");
    return pet;
  }

  private static VetScheduleSlot slot(Long id, Long vetId) {
    VetScheduleSlot slot = new VetScheduleSlot();
    slot.setId(id);
    slot.setVetId(vetId);
    slot.setStatus("AVAILABLE");
    return slot;
  }

  private static Visit scheduledVisit(
      Long id, Long createdBy, Long petId, Long slotId) {
    Visit visit = new Visit();
    visit.setId(id);
    visit.setCreatedBy(createdBy);
    visit.setPetId(petId);
    visit.setSlotId(slotId);
    visit.setStatus("SCHEDULED");
    return visit;
  }
}
