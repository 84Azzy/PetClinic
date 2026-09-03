package com.zzy.petclinic.pet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.owner.OwnerService;
import com.zzy.petclinic.owner.Owner;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.GetMapping;

/** Pet 模块业务验收；每个失败用例都对应一个待完成的契约。 */
class PetModuleAcceptanceTest {
  private PetMapper mapper;
  private CurrentUser currentUser;
  private OwnerService ownerService;
  private PetServiceImpl service;

  @BeforeEach
  void setUp() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), "pet-test"), Pet.class);
    mapper = mock(PetMapper.class);
    currentUser = mock(CurrentUser.class);
    ownerService = mock(OwnerService.class);
    service = new PetServiceImpl(mapper, currentUser, ownerService);
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createReturnsAnActivePet() {
    Pet pet = service.create(minimalRequest());

    assertEquals("ACTIVE", pet.getStatus(), "新增宠物应在返回前显式初始化为 ACTIVE");
  }

  @Test
  void createEndpointReturnsCreatedResponse() {
    PetController controller = new PetController();
    PetService petService = mock(PetService.class);
    ReflectionTestUtils.setField(controller, "petService", petService);
    Pet created = new Pet();
    when(petService.create(minimalRequest())).thenReturn(created);

    assertEquals(
        201,
        controller.create(minimalRequest()).code(),
        "POST /api/pets 应使用 ApiResponse.created 返回 201 创建语义");
  }

  @Test
  void adminGetEndpointReturnsTheServiceResult() {
    PetController controller = new PetController();
    PetService petService = mock(PetService.class);
    ReflectionTestUtils.setField(controller, "petService", petService);
    Pet expected = new Pet();
    when(petService.get(8L)).thenReturn(expected);

    Pet actual =
        assertDoesNotThrow(
                () -> controller.get(8L),
                "ADMIN 查到宠物后 Controller 应立即 return，不能继续落入 403 分支")
            .data();
    assertSame(expected, actual);
  }

  @Test
  void defaultPageExcludesInactivePets() {
    when(currentUser.require()).thenReturn(authenticatedUser(1L, "ADMIN"));
    when(mapper.selectPage(any(Page.class), any(Wrapper.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.page(new PageQuery(1L, 10L, null, null), null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Wrapper<Pet>> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(mapper).selectPage(any(Page.class), captor.capture());
    LambdaQueryWrapper<Pet> wrapper = (LambdaQueryWrapper<Pet>) captor.getValue();
    assertTrue(
        wrapper.getSqlSegment().contains("status")
            && wrapper.getParamNameValuePairs().containsValue("ACTIVE"),
        "默认分页必须加上 status = ACTIVE，否则已停用宠物仍会出现");
  }

  @Test
  void requestedStatusIsAppliedToPage() {
    when(currentUser.require()).thenReturn(authenticatedUser(1L, "ADMIN"));
    when(mapper.selectPage(any(Page.class), any(Wrapper.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.page(new PageQuery(1L, 10L, null, "INACTIVE"), null);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Wrapper<Pet>> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(mapper).selectPage(any(Page.class), captor.capture());
    LambdaQueryWrapper<Pet> wrapper = (LambdaQueryWrapper<Pet>) captor.getValue();
    assertTrue(
        wrapper.getSqlSegment().contains("status")
            && wrapper.getParamNameValuePairs().containsValue("INACTIVE"),
        "PageQuery.status 必须参与分页查询，否则状态筛选参数无效");
  }

  @Test
  void missingPetReturns404() {
    when(mapper.selectById(404L)).thenReturn(null);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.get(404L),
            "查询不存在的宠物应抛出 BusinessException(404)");
    assertEquals(404, exception.getStatus().value());
  }

  @Test
  void updatingMissingPetReturns404() {
    when(mapper.selectById(404L)).thenReturn(null);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.update(404L, minimalRequest()),
            "修改不存在的宠物应抛出 BusinessException(404)，不能出现空指针");
    assertEquals(404, exception.getStatus().value());
  }

  @Test
  void deletingMissingPetReturns404() {
    when(mapper.selectById(404L)).thenReturn(null);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.delete(404L),
            "停用不存在的宠物应抛出 BusinessException(404)，不能出现空指针");
    assertEquals(404, exception.getStatus().value());
  }

  @Test
  void deletingPetUsesTheSchemaStatusValueInactive() {
    Pet pet = new Pet();
    pet.setId(8L);
    pet.setStatus("ACTIVE");
    when(mapper.selectById(8L)).thenReturn(pet);

    service.delete(8L);

    assertEquals("INACTIVE", pet.getStatus(), "停用状态应为 INACTIVE，不是 UNACTIVE");
  }

  @Test
  void ownerCannotReadAnotherOwnersPet() {
    Pet foreignPet = foreignPet();
    when(mapper.selectById(8L)).thenReturn(foreignPet);
    when(currentUser.require()).thenReturn(authenticatedUser(3L, "OWNER"));
    Owner owner = new Owner();
    owner.setId(1L);
    when(ownerService.mine()).thenReturn(owner);

    assertThrows(
        AccessDeniedException.class,
        () -> service.get(8L),
        "OWNER 读取其他宠主的宠物必须返回 403");
  }

  @Test
  void updateRequiresAdminOrStaffAnnotation() throws Exception {
    assertEquals(
        "hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')",
        PetServiceImpl.class
            .getMethod("update", Long.class, PetRequest.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class)
            .value());
  }

  @Test
  void deleteRequiresAdminOrStaffAnnotation() throws Exception {
    assertEquals(
        "hasAuthority('pet:delete') && hasAnyRole('ADMIN', 'STAFF')",
        PetServiceImpl.class
            .getMethod("delete", Long.class)
            .getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class)
            .value());
  }

  @Test
  void mineUsesMapperQueryBoundToCurrentUserId() {
    Pet pet = new Pet();
    when(currentUser.id()).thenReturn(3L);
    when(mapper.selectMine(3L)).thenReturn(List.of(pet));

    List<Pet> result = service.mine();

    assertEquals(
        List.of(pet),
        result,
        "mine 应调用 PetMapper.selectMine(userId) 走 owner.user_id 归属查询");
    verify(mapper).selectMine(3L);
  }

  @Test
  void mineEndpointDoesNotAcceptAnOwnerIdFromTheClient() throws Exception {
    Method method =
        Arrays.stream(PetController.class.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals("mine"))
            .findFirst()
            .orElseThrow();
    GetMapping mapping = method.getAnnotation(GetMapping.class);

    assertArrayEquals(
        new String[] {"/mine"},
        mapping.value(),
        "“我的宠物”应是 /api/pets/mine，不应让客户端提供 ownerId");
    assertFalse(
        Arrays.stream(method.getParameters())
            .anyMatch(parameter -> parameter.getType().equals(Long.class)),
        "mine 不应接收客户端传入的 ownerId");
  }

  private static PetRequest minimalRequest() {
    return new PetRequest(
        1L,
        1L,
        "糯米",
        "FEMALE",
        "英短",
        LocalDate.of(2022, 4, 12),
        null,
        null,
        null,
        null);
  }

  private static Pet foreignPet() {
    Pet pet = new Pet();
    pet.setId(8L);
    pet.setOwnerId(2L);
    pet.setStatus("ACTIVE");
    return pet;
  }

  private static AuthenticatedUser authenticatedUser(Long userId, String accountType) {
    SysUser entity = new SysUser();
    entity.setId(userId);
    entity.setUsername(accountType.toLowerCase() + "-" + userId);
    entity.setPasswordHash("password");
    entity.setAccountType(accountType);
    entity.setStatus("ACTIVE");
    return new AuthenticatedUser(
        entity, new UserAuthorities(Set.of(accountType), Set.of("pet:manage")));
  }
}
