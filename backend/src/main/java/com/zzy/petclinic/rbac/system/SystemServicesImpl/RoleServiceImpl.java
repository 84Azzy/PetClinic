package com.zzy.petclinic.rbac.system.SystemServicesImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.rbac.system.SysPermission;
import com.zzy.petclinic.rbac.system.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.SysRole;
import com.zzy.petclinic.rbac.system.SysRoleMapper;
import com.zzy.petclinic.rbac.system.SystemRequests;
import com.zzy.petclinic.rbac.system.SystemServices;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
// 不恰当：项目实际权限码是 system:manage，system:manager 会让合法管理员也被拒绝。
// @PreAuthorize("hasAnyAuthority('system:manager')")
@PreAuthorize("hasAuthority('system:manage')")
public class RoleServiceImpl implements SystemServices.RoleService {

    private static final Set<String> RESERVED_ROLE_CODES = Set.of("ADMIN", "STAFF", "OWNER");
    private static final Set<String> ROLE_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;


    /**
     * 查询全部角色。
     *
     * <p>实现提示：使用 MyBatis-Plus {@code selectList}，按 id 或 code 排序。后台管理页通常需要同时看到 ACTIVE 和 INACTIVE，不能默认只查
     * ACTIVE。
     *
     * @return 角色列表
     */
    @Override
    public List<SysRole> list() {
        // 不恰当：同时按 id、code 倒序没有实际意义，也与系统其他管理列表的稳定顺序不一致。
        // return sysRoleMapper.selectList(
        //         new LambdaQueryWrapper<SysRole>().orderByDesc(SysRole::getId, SysRole::getCode));
        return sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    /**
     * 新建角色。
     *
     * <p>实现提示：检查 code 唯一；复制 code、name、description；status 为空时使用 ACTIVE，并只接受 ACTIVE/INACTIVE；插入后返回。
     * 并发下仍要把数据库唯一键异常转换成 409。
     *
     * @param r 角色资料
     * @return 新建角色
     */
    @Override
    public SysRole create(SystemRequests.RoleSave r) {
        validateRoleSave(r);
        String code = r.code().trim();
        if (isReservedCode(code)) {
            throw BusinessException.badRequest("内置角色编码不能由接口创建");
        }
        ensureCodeAvailable(code, null);

        SysRole role = new SysRole();
        role.setCode(code);
        role.setName(r.name().trim());
        role.setDescription(r.description());
        role.setStatus(statusForCreate(r.status()));
        LocalDateTime now = LocalDateTime.now();
        role.setCreatedAt(now);
        role.setUpdatedAt(now);

        /*
         * 不恰当：没有在插入前检查 code，且 createdAt/updatedAt 在 insert 之后赋值，不会写入数据库。
         * sysRoleMapper.insert(role);
         * role.setCreatedAt(LocalDateTime.now());
         */
        try {
            if (sysRoleMapper.insert(role) != 1) {
                throw BusinessException.conflict("角色插入失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("角色编码已存在");
        }
        return role;
    }

    /**
     * 修改角色。
     *
     * <p>实现提示：按 id 查询并返回 404；code 变化时检查唯一性；校验状态值后更新允许修改的字段。若项目把 ADMIN 等内置角色视为保留角色，
     * 应禁止修改其 code。
     *
     * @param roleId 角色编号
     * @param r  新角色资料
     * @return 修改后的角色
     */
    @Override
    public SysRole update(Long roleId, SystemRequests.RoleSave r) {
        SysRole role = requiredRole(roleId);
        validateRoleSave(r);
        String code = r.code().trim();

        /*
         * 不恰当：只要是内置角色就禁止修改任何资料；真正需要保持稳定的是内置 role code。
         * if (RESERVED_ROLE_CODES.contains(role.getCode())) {
         *     throw BusinessException.badRequest("系统内置角色禁止修改");
         * }
         */
        //当前角色为内置角色
        if (isReservedCode(role.getCode()) && !role.getCode().equals(code)) {
            throw BusinessException.badRequest("系统内置角色编码禁止修改");
        }
        //普通角色
        if (!role.getCode().equals(code)) {
            if (isReservedCode(code)) {
                throw BusinessException.badRequest("不能使用内置角色编码");
            }
            ensureCodeAvailable(code, roleId);
            role.setCode(code);
        }
        if (r.status() != null) {
            String status = r.status().trim();
            validateStatus(status);
            role.setStatus(status);
        }

        // 不恰当：原实现遗漏 name，且 hasText(description) 导致描述无法被清空。
        // if (StringUtils.hasText(r.description())) role.setDescription(r.description());
        role.setName(r.name().trim());
        role.setDescription(r.description());
        role.setUpdatedAt(LocalDateTime.now());
        try {
            if (sysRoleMapper.updateById(role) != 1) {
                throw BusinessException.conflict("角色更新失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("角色编码已存在");
        }
        return role;
    }

    /**
     * 删除角色。
     *
     * <p>实现提示（需要事务）：先确认角色存在；内置角色可直接禁止删除；若仍有用户绑定该角色，返回 409 并提示先解绑；清理
     * sys_role_permission 后再删除角色。不要依赖数据库外键异常作为正常业务流程。
     *
     * @param roleId 角色编号
     */
    @Override
    @Transactional
    public void delete(Long roleId) {
        SysRole role = requiredRole(roleId);
        if (isReservedCode(role.getCode())) {
            throw BusinessException.badRequest("内置角色禁止删除");
        }

        // 不恰当：角色仍被用户引用属于资源状态冲突，应返回 409，而不是 400 参数错误。
        // throw BusinessException.badRequest("还有用户关联该角色，请先解除关系再删除");
        if (sysRoleMapper.countUsersByRoleId(roleId) > 0) {
            throw BusinessException.conflict("还有用户关联该角色，请先解除关系再删除");
        }
        sysRoleMapper.deletePermissionsByRoleId(roleId);
        if (sysRoleMapper.deleteById(roleId) != 1) {
            throw BusinessException.conflict("角色删除失败，请重试");
        }
    }

    /**
     * 全量替换角色拥有的权限。
     *
     * <p>实现提示（需要事务）：确认角色存在；对 ids 去重并拒绝 null；批量确认权限 id 全部存在；删除该角色原有
     * sys_role_permission，再批量插入新关系；空列表表示清空。当前 JWT 过滤器每次请求都会重新查权，所以变更会在下一次请求生效；不要在 JWT
     * 中复制一份长期不刷新的权限列表。
     *
     * @param roleId 角色编号
     * @param ids 完整的目标权限编号集合
     */
    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> ids) {
        requiredRole(roleId);
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw BusinessException.badRequest("权限编号集合及其元素不能为空");
        }
        List<Long> permissionIds = List.copyOf(new LinkedHashSet<>(ids));

        if (!permissionIds.isEmpty()) {
            Map<Long, SysPermission> permissionsById =
                    sysPermissionMapper.selectByIds(permissionIds).stream()
                            .collect(Collectors.toMap(SysPermission::getId, Function.identity()));
            for (Long permissionId : permissionIds) {
                if (!permissionsById.containsKey(permissionId)) {
                    throw BusinessException.notFound("权限 " + permissionId);
                }
            }
        }

        /*
         * 不恰当：分配阶段拒绝 INACTIVE 权限会导致角色无法保存已有的停用权限；
         * 权限是否生效已经由授权查询中的 p.status = 'ACTIVE' 控制，这里只校验权限存在。
         * if (!"ACTIVE".equals(permission.getStatus())) { throw ...; }
         */
        sysRoleMapper.deletePermissionsByRoleId(roleId);
        if (!permissionIds.isEmpty()
                && sysRoleMapper.insertRolePermissions(roleId, permissionIds)
                        != permissionIds.size()) {
            throw BusinessException.conflict("权限分配失败，请重试");
        }
    }

    private SysRole requiredRole(Long roleId) {
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null) {
            // 不恰当：notFound 会自动追加“不存在”，传入“该角色不存在”会得到重复文案。
            // throw BusinessException.notFound("该角色不存在");
            throw BusinessException.notFound("角色");
        }
        return role;
    }

    private void validateStatus(String status) {
        if (!StringUtils.hasText(status) || !ROLE_STATUSES.contains(status)) {
            throw BusinessException.badRequest("角色状态只能为 ACTIVE 或 INACTIVE");
        }
    }

    private String statusForCreate(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim();
        validateStatus(normalized);
        return normalized;
    }

    private void validateRoleSave(SystemRequests.RoleSave request) {
        if (request == null
                || !StringUtils.hasText(request.code())
                || !StringUtils.hasText(request.name())) {
            throw BusinessException.badRequest("角色编码和名称不能为空");
        }
    }

    private void ensureCodeAvailable(String code, Long excludedId) {
        LambdaQueryWrapper<SysRole> query =
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, code);
        if (excludedId != null) {
            query.ne(SysRole::getId, excludedId);
        }
        if (sysRoleMapper.selectOne(query) != null) {
            throw BusinessException.conflict("角色编码已存在");
        }
    }

    /**
     * code有内容且内容为内置角色
     * @param code
     * @return
     */
    private boolean isReservedCode(String code) {
        return StringUtils.hasText(code)
                && RESERVED_ROLE_CODES.contains(code.trim().toUpperCase(java.util.Locale.ROOT));
    }
}
