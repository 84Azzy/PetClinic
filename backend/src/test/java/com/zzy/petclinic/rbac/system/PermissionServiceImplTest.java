package com.zzy.petclinic.rbac.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.system.dataObject.SysPermission;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.mapper.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.systemServices.impl.PermissionServiceImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PermissionServiceImplTest {
  private SysPermissionMapper mapper;
  private CurrentUser currentUser;
  private PermissionServiceImpl service;

  @BeforeEach
  void setUp() {
    mapper = mock(SysPermissionMapper.class);
    currentUser = mock(CurrentUser.class);
    service = new PermissionServiceImpl(mapper, currentUser);
  }

  @Test
  void treeBuildsHierarchyAndSortsEveryLevel() {
    SysPermission laterRoot = permission(1L, null, "later", 20, "MENU");
    SysPermission secondChild = permission(3L, 1L, "second", 20, "BUTTON");
    SysPermission firstRoot = permission(4L, null, "first-root", 5, "MENU");
    SysPermission firstChild = permission(2L, 1L, "first", 10, "BUTTON");
    when(mapper.selectList(any(Wrapper.class)))
        .thenReturn(List.of(laterRoot, secondChild, firstRoot, firstChild));

    List<SysPermission> tree = service.tree();

    assertEquals(List.of(4L, 1L), tree.stream().map(SysPermission::getId).toList());
    assertEquals(
        List.of(2L, 3L),
        tree.get(1).getChildren().stream().map(SysPermission::getId).toList());
  }

  @Test
  void mineUsesAuthenticatedUserIdAndKeepsGrantedOrphanPermission() {
    SysPermission orphan = permission(8L, 99L, "visit:create", 1, "BUTTON");
    when(currentUser.id()).thenReturn(7L);
    when(mapper.selectByUserId(7L)).thenReturn(List.of(orphan));

    List<SysPermission> mine = service.mine();

    assertEquals(List.of(8L), mine.stream().map(SysPermission::getId).toList());
    assertEquals(List.of(), mine.get(0).getChildren());
    verify(mapper).selectByUserId(7L);
  }

  @Test
  void createNormalizesFieldsAndWritesAuditTime() {
    SystemRequests.PermissionSave request =
        new SystemRequests.PermissionSave(
            null, " pet:export ", " 导出宠物 ", " button ", null, null, null, null);
    when(mapper.selectOne(any(Wrapper.class))).thenReturn(null);
    when(mapper.insert(any(SysPermission.class))).thenReturn(1);

    SysPermission created = service.create(request);

    assertEquals("pet:export", created.getCode());
    assertEquals("导出宠物", created.getName());
    assertEquals("BUTTON", created.getType());
    assertEquals("ACTIVE", created.getStatus());
    assertEquals(0, created.getSortOrder());
    assertNotNull(created.getCreatedAt());
    assertNotNull(created.getUpdatedAt());
  }

  @Test
  void updateRejectsMovingNodeBelowItsDescendant() {
    SysPermission root = permission(1L, null, "root", 1, "MENU");
    SysPermission descendant = permission(3L, 1L, "child", 1, "MENU");
    when(mapper.selectById(1L)).thenReturn(root);
    when(mapper.selectById(3L)).thenReturn(descendant);

    SystemRequests.PermissionSave request =
        new SystemRequests.PermissionSave(3L, "root", "根节点", "MENU", null, null, 1, "ACTIVE");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.update(1L, request));

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    verify(mapper, never()).update(any(), any());
  }

  @Test
  void deleteRejectsPermissionThatStillHasChildren() {
    when(mapper.selectById(5L)).thenReturn(permission(5L, null, "parent", 1, "MENU"));
    when(mapper.countChildrenByParentId(5L)).thenReturn(1L);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.delete(5L));

    assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    verify(mapper, never()).deleteRoleRelationsByPermissionId(5L);
    verify(mapper, never()).deleteById(5L);
  }

  @Test
  void deleteClearsRoleRelationsBeforeDeletingPermission() {
    when(mapper.selectById(5L)).thenReturn(permission(5L, null, "leaf", 1, "BUTTON"));
    when(mapper.countChildrenByParentId(5L)).thenReturn(0L);
    when(mapper.deleteById(5L)).thenReturn(1);

    service.delete(5L);

    var order = inOrder(mapper);
    order.verify(mapper).deleteRoleRelationsByPermissionId(5L);
    order.verify(mapper).deleteById(5L);
  }

  private static SysPermission permission(
      Long id, Long parentId, String code, Integer sortOrder, String type) {
    SysPermission permission = new SysPermission();
    permission.setId(id);
    permission.setParentId(parentId);
    permission.setCode(code);
    permission.setName(code);
    permission.setSortOrder(sortOrder);
    permission.setType(type);
    permission.setStatus("ACTIVE");
    return permission;
  }
}
