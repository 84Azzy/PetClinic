package com.zzy.petclinic.system;

import com.zzy.petclinic.authentication.SysUser;
import com.zzy.petclinic.common.*;
import java.util.List;

public final class SystemServices {
  private SystemServices() {}

  public interface UserAdminService {
    PageResponse<SysUser> page(PageQuery q);

    SysUser create(SystemRequests.UserSave r);

    SysUser update(Long id, SystemRequests.UserSave r);

    void changeStatus(Long id, String status);

    void resetPassword(Long id, String password);

    void assignRoles(Long id, List<Long> ids);

    List<SysRole> roles(Long id);
  }

  public interface RoleService {
    List<SysRole> list();

    SysRole create(SystemRequests.RoleSave r);

    SysRole update(Long id, SystemRequests.RoleSave r);

    void delete(Long id);

    void assignPermissions(Long id, List<Long> ids);
  }

  public interface PermissionService {
    List<SysPermission> tree();

    List<SysPermission> mine();

    SysPermission create(SystemRequests.PermissionSave r);

    SysPermission update(Long id, SystemRequests.PermissionSave r);

    void delete(Long id);
  }
}
