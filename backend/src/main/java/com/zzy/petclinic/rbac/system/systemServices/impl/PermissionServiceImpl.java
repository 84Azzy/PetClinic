package com.zzy.petclinic.rbac.system.systemServices.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.system.dataObject.SysPermission;
import com.zzy.petclinic.rbac.system.mapper.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.systemServices.SystemServices;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:manage')")
public class PermissionServiceImpl implements SystemServices.PermissionService {

    private static final Set<String> PERMISSION_TYPES =
            Set.of("MENU", "BUTTON", "API");

    private static final Set<String> PERMISSION_STATUSES =
            Set.of("ACTIVE", "INACTIVE");

    private static final Comparator<SysPermission> PERMISSION_ORDER =
            Comparator.comparing(
                            (SysPermission permission) ->
                                    Objects.requireNonNullElse(permission.getSortOrder(), 0))
                    .thenComparing(SysPermission::getId);

    private final SysPermissionMapper sysPermissionMapper;
    private final CurrentUser currentUser;

    /**
     * 查询完整权限树。
     *
     * 管理端查询的是数据库中的完整权限结构，因此遇到孤儿节点时直接报错。
     */
    @Override
    public List<SysPermission> tree() {
        List<SysPermission> permissions =
                sysPermissionMapper.selectList(
                        new LambdaQueryWrapper<SysPermission>()
                                .orderByAsc(SysPermission::getSortOrder)
                                .orderByAsc(SysPermission::getId));

        return buildTree(permissions, true);
    }

    /**
     * 查询当前用户的权限树。
     *
     * 用户可能只拥有子权限而没有父权限，因此允许把缺少父节点的权限提升为返回结果的根节点。
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public List<SysPermission> mine() {
        Long userId = currentUser.id();
        List<SysPermission> permissions =
                sysPermissionMapper.selectByUserId(userId);

        return buildTree(permissions, false);
    }

    /** 新建权限。 */
    @Override
    @Transactional
    public SysPermission create(SystemRequests.PermissionSave request) {
        validateRequest(request);

        String code = request.code().trim();
        String name = request.name().trim();
        String type = normalizeType(request.type());
        String status = statusForCreate(request.status());
        int sortOrder = Objects.requireNonNullElse(request.sortOrder(), 0);

        ensureCodeAvailable(code, null);
        ensureParentExists(request.parentId());

        SysPermission permission = new SysPermission();
        permission.setParentId(request.parentId());
        permission.setCode(code);
        permission.setName(name);
        permission.setType(type);
        permission.setPath(request.path());
        permission.setIcon(request.icon());
        permission.setSortOrder(sortOrder);
        permission.setStatus(status);

        LocalDateTime now = LocalDateTime.now();
        permission.setCreatedAt(now);
        permission.setUpdatedAt(now);

        try {
            if (sysPermissionMapper.insert(permission) != 1) {
                throw BusinessException.conflict("权限创建失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("权限编码已存在");
        }

        return permission;
    }

    /** 修改权限。 */
    @Override
    @Transactional
    public SysPermission update(
            Long permissionId,
            SystemRequests.PermissionSave request) {

        SysPermission permission = requiredPermission(permissionId);
        validateRequest(request);

        String code = request.code().trim();
        String name = request.name().trim();
        String type = normalizeType(request.type());

        if (!Objects.equals(permission.getCode(), code)) {
            ensureCodeAvailable(code, permissionId);
        }

        /*
         * 新父节点不能是当前节点，也不能是当前节点的后代。
         *
         * 例如：
         * A
         * └── B
         *     └── C
         *
         * 修改 A 时，不能把 C 设为 A 的父节点，否则会形成：
         * A → B → C → A
         */
        ensureParentDoesNotCreateCycle(
                permissionId,
                request.parentId());

        String status =
                request.status() == null
                        ? permission.getStatus()
                        : normalizeStatus(request.status());

        int sortOrder =
                request.sortOrder() == null
                        ? Objects.requireNonNullElse(permission.getSortOrder(), 0)
                        : request.sortOrder();

        permission.setParentId(request.parentId());
        permission.setCode(code);
        permission.setName(name);
        permission.setType(type);
        permission.setPath(request.path());
        permission.setIcon(request.icon());
        permission.setSortOrder(sortOrder);
        permission.setStatus(status);
        permission.setUpdatedAt(LocalDateTime.now());
        permission.setChildren(new ArrayList<>());

        /*
         * 不直接使用 updateById(permission)。
         *
         * MyBatis-Plus 默认可能忽略值为 null 的字段，导致：
         * 1. parentId 无法清空，节点不能移动到根节点；
         * 2. path、icon 无法清空。
         *
         * LambdaUpdateWrapper.set(...) 会明确生成 SET column = NULL。
         */
        LambdaUpdateWrapper<SysPermission> update =
                new LambdaUpdateWrapper<SysPermission>()
                        .eq(SysPermission::getId, permissionId)
                        .set(SysPermission::getParentId, request.parentId())
                        .set(SysPermission::getCode, code)
                        .set(SysPermission::getName, name)
                        .set(SysPermission::getType, type)
                        .set(SysPermission::getPath, request.path())
                        .set(SysPermission::getIcon, request.icon())
                        .set(SysPermission::getSortOrder, sortOrder)
                        .set(SysPermission::getStatus, status)
                        .set(SysPermission::getUpdatedAt, permission.getUpdatedAt());

        try {
            if (sysPermissionMapper.update(null, update) != 1) {
                throw BusinessException.conflict("权限更新失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("权限编码已存在");
        }

        return permission;
    }

    /** 删除权限。 */
    @Override
    @Transactional
    public void delete(Long permissionId) {
        requiredPermission(permissionId);

        long childCount =
                sysPermissionMapper.countChildrenByParentId(permissionId);

        if (childCount > 0) {
            throw BusinessException.conflict("该权限仍有子节点，请先处理子节点");
        }

        sysPermissionMapper.deleteRoleRelationsByPermissionId(permissionId);

        if (sysPermissionMapper.deleteById(permissionId) != 1) {
            throw BusinessException.conflict("权限删除失败，请重试");
        }
    }

    /**
     * 将扁平权限列表构建成树。
     *
     * @param flatPermissions 扁平权限列表
     * @param rejectOrphans 是否拒绝孤儿节点
     */
    private List<SysPermission> buildTree(
            List<SysPermission> flatPermissions,
            boolean rejectOrphans) {

        if (flatPermissions == null || flatPermissions.isEmpty()) {
            return List.of();
        }

        /*
         * 第一步：根据 id 建立索引。
         *
         * LinkedHashMap 可以保留放入顺序，不过后面仍会显式排序。
         */
        Map<Long, SysPermission> nodesById = new LinkedHashMap<>();

        for (SysPermission permission : flatPermissions) {
            if (permission.getId() == null) {
                throw BusinessException.conflict("权限树中存在没有 id 的节点");
            }

            SysPermission old =
                    nodesById.put(permission.getId(), permission);

            if (old != null) {
                throw BusinessException.conflict(
                        "权限树中存在重复节点：" + permission.getId());
            }

            // 避免重复调用 tree() 时残留旧 children。
            permission.setChildren(new ArrayList<>());
        }

        /*
         * 第二步：稳定排序。
         *
         * 根节点和每一层子节点都按 sortOrder、id 排序。
         */
        List<SysPermission> ordered =
                new ArrayList<>(nodesById.values());

        ordered.sort(PERMISSION_ORDER);

        /*
         * 第三步：建立 parentId -> children 的邻接表，并找出根节点。
         */
        Map<Long, List<SysPermission>> childrenByParentId =
                new HashMap<>();

        List<SysPermission> roots = new ArrayList<>();

        for (SysPermission permission : ordered) {
            Long parentId = permission.getParentId();

            if (parentId == null) {
                roots.add(permission);
                continue;
            }

            SysPermission parent = nodesById.get(parentId);

            if (parent == null) {
                if (rejectOrphans) {
                    throw BusinessException.conflict(
                            "权限 "
                                    + permission.getId()
                                    + " 的父节点 "
                                    + parentId
                                    + " 不存在");
                }

                /*
                 * mine() 查询出的权限可能没有包含其父权限。
                 * 不能静默丢掉这个合法权限，因此把它提升为当前结果的根节点。
                 */
                roots.add(permission);
                continue;
            }

            childrenByParentId
                    .computeIfAbsent(parentId, ignored -> new ArrayList<>())
                    .add(permission);
        }

        /*
         * 第四步：从每个根节点开始递归挂载 children。
         */
        Set<Long> visiting = new HashSet<>();
        Set<Long> visited = new HashSet<>();

        for (SysPermission root : roots) {
            attachChildren(
                    root,
                    childrenByParentId,
                    visiting,
                    visited);
        }

        /*
         * 如果还有节点没有从任何根节点访问到，通常意味着这些节点组成了一个环。
         *
         * 例如：
         * A.parentId = B.id
         * B.parentId = A.id
         *
         * A、B 都不是根节点，因此 roots 为空，但 nodesById 不为空。
         */
        if (visited.size() != nodesById.size()) {
            throw BusinessException.conflict("权限树存在循环父子关系");
        }

        return roots;
    }

    /**
     * 递归挂载一个节点的全部子节点。
     */
    private SysPermission attachChildren(
            SysPermission current,
            Map<Long, List<SysPermission>> childrenByParentId,
            Set<Long> visiting,
            Set<Long> visited) {

        Long currentId = current.getId();

        /*
         * visiting 表示当前递归调用链中的节点。
         * 如果一个节点在同一条递归链中出现两次，就说明出现了环。
         */
        if (!visiting.add(currentId)) {
            throw BusinessException.conflict(
                    "权限树存在循环父子关系，节点：" + currentId);
        }

        List<SysPermission> children =
                childrenByParentId.getOrDefault(
                        currentId,
                        List.of());

        List<SysPermission> attachedChildren =
                new ArrayList<>(children.size());

        for (SysPermission child : children) {
            attachedChildren.add(
                    attachChildren(
                            child,
                            childrenByParentId,
                            visiting,
                            visited));
        }

        current.setChildren(attachedChildren);

        visiting.remove(currentId);
        visited.add(currentId);

        return current;
    }

    /**
     * 修改父节点时，沿父节点链向上查找。
     *
     * 如果最终又找到了当前节点，就说明会形成环。
     */
    private void ensureParentDoesNotCreateCycle(
            Long permissionId,
            Long newParentId) {

        if (newParentId == null) {
            return;
        }

        Set<Long> visited = new HashSet<>();
        Long cursor = newParentId;

        while (cursor != null) {
            if (Objects.equals(cursor, permissionId)) {
                throw BusinessException.conflict(
                        "不能把当前权限或其后代设置为父节点");
            }

            /*
             * 如果数据库中本来就存在环，也要终止遍历，
             * 防止 while 无限循环。
             */
            if (!visited.add(cursor)) {
                throw BusinessException.conflict(
                        "现有权限数据中存在循环父子关系");
            }

            SysPermission parent =
                    sysPermissionMapper.selectById(cursor);

            if (parent == null) {
                throw BusinessException.notFound(
                        "父权限 " + cursor);
            }

            cursor = parent.getParentId();
        }
    }

    private SysPermission requiredPermission(Long permissionId) {
        SysPermission permission =
                sysPermissionMapper.selectById(permissionId);

        if (permission == null) {
            throw BusinessException.notFound("权限");
        }

        return permission;
    }

    private void ensureParentExists(Long parentId) {
        if (parentId == null) {
            return;
        }

        if (sysPermissionMapper.selectById(parentId) == null) {
            throw BusinessException.notFound(
                    "父权限 " + parentId);
        }
    }

    private void ensureCodeAvailable(
            String code,
            Long excludedPermissionId) {

        LambdaQueryWrapper<SysPermission> query =
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getCode, code);

        if (excludedPermissionId != null) {
            query.ne(
                    SysPermission::getId,
                    excludedPermissionId);
        }

        if (sysPermissionMapper.selectOne(query) != null) {
            throw BusinessException.conflict("权限编码已存在");
        }
    }

    private void validateRequest(
            SystemRequests.PermissionSave request) {

        if (request == null
                || !StringUtils.hasText(request.code())
                || !StringUtils.hasText(request.name())
                || !StringUtils.hasText(request.type())) {

            throw BusinessException.badRequest(
                    "权限编码、名称和类型不能为空");
        }
    }

    private String normalizeType(String type) {
        String normalized =
                type.trim().toUpperCase(Locale.ROOT);

        if (!PERMISSION_TYPES.contains(normalized)) {
            throw BusinessException.badRequest(
                    "权限类型只能为 MENU、BUTTON 或 API");
        }

        return normalized;
    }

    private String statusForCreate(String status) {
        if (!StringUtils.hasText(status)) {
            return "ACTIVE";
        }

        return normalizeStatus(status);
    }

    private String normalizeStatus(String status) {
        String normalized =
                status.trim().toUpperCase(Locale.ROOT);

        if (!PERMISSION_STATUSES.contains(normalized)) {
            throw BusinessException.badRequest(
                    "权限状态只能为 ACTIVE 或 INACTIVE");
        }

        return normalized;
    }
}