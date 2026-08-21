package com.zzy.petclinic.rbac;

import com.zzy.petclinic.auth.SysUser;
import com.zzy.petclinic.common.*;
import java.util.List;

public final class RbacServices {
  private RbacServices() {}

  public interface UserAdminService {
    PageResponse<SysUser> page(PageQuery q);

    SysUser create(RbacRequests.UserSave r);

    SysUser update(Long id, RbacRequests.UserSave r);

    void changeStatus(Long id, String status);

    void resetPassword(Long id, String password);

    void assignRoles(Long id, List<Long> ids);

    List<SysRole> roles(Long id);
  }

  public interface RoleService {
    List<SysRole> list();

    SysRole create(RbacRequests.RoleSave r);

    SysRole update(Long id, RbacRequests.RoleSave r);

    void delete(Long id);

    void assignPermissions(Long id, List<Long> ids);
  }

  public interface PermissionService {
    List<SysPermission> tree();

    List<SysPermission> mine();

    SysPermission create(RbacRequests.PermissionSave r);

    SysPermission update(Long id, RbacRequests.PermissionSave r);

    void delete(Long id);
  }
}
