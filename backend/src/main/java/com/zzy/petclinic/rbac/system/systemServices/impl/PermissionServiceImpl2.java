package com.zzy.petclinic.rbac.system.systemServices.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.system.dataObject.SysPermission;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.mapper.SysPermissionMapper;
import com.zzy.petclinic.rbac.system.systemServices.SystemServices;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 简化版的权限service实现
 */
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:manage')")
public class PermissionServiceImpl2 implements SystemServices.PermissionService {

    private static final Set<String> PERMISSION_TYPES=Set.of("MENU","BUTTON","API");
    private static final Set<String> PERMISSION_STATUSES=Set.of("ACTIVE","INACTIVE");

    private final SysPermissionMapper sysPermissionMapper;
    private final CurrentUser currentUser;
    /**
     * 查询完整权限树。
     *
     * <p>实现提示：查询全部权限并按 sortOrder、id 排序；以 parentId 分组，再从 parentId 为空的根节点递归挂载子节点。构建前应检测孤儿节点和
     * 环，避免无限递归。
     *
     * <p><strong>当前契约缺口：</strong>{@link SysPermission} 没有 children 字段。真正返回树之前，应新增权限树 DTO，或添加
     * {@code @TableField(exist = false) List<SysPermission> children}；否则本方法只能返回排序后的扁平列表。
     *
     * @return 权限根节点列表，每个根节点包含其后代
     */
    @Override
    public List<SysPermission> tree() {
        List<SysPermission>permissions = sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .orderByAsc(SysPermission::getSortOrder)
                        .orderByAsc(SysPermission::getId)
        );
        return buildTree(permissions);
    }

    /**
     * 查询当前登录用户可用的权限树。
     *
     * <p>实现提示：从 {@code CurrentUser.id()} 获取用户编号，不接收前端 userId；调用
     * {@link SysPermissionMapper#selectByUserId(Long)} 得到已启用权限；再使用与 {@link #tree()} 相同的组树函数，避免复制递归逻辑。
     *
     * @return 当前用户拥有的权限树
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public List<SysPermission> mine() {
        Long userId = currentUser.id();
        List<SysPermission> permissions = sysPermissionMapper.selectByUserId(userId);
        return buildTree(permissions);
    }

    /**
     * 新建权限节点。
     *
     * <p>实现提示：检查 code 唯一；parentId 非空时确认父节点存在；type 只接受 MENU/BUTTON/API；复制 path、icon；sortOrder 为空用 0，
     * status 为空用 ACTIVE；插入后返回。并发重复 code 转换成 409。
     *
     * @param r 权限节点资料
     * @return 新建权限
     */
    @Override
    public SysPermission create(SystemRequests.PermissionSave r) {
        validateRequest(r);
        String code=r.code().trim();
        String name =r.name().trim();
        String type=normalizeType(r.type());
        String status=statusForCreate(r.status());
        int sortOrder= Objects.requireNonNullElse(r.sortOrder(),0);

        ensureCodeAvailable(code,null);
        ensureParentExists(r.parentId());

        SysPermission permission = new SysPermission();
        permission.setParentId(r.parentId());
        permission.setCode(code);
        permission.setName(name);
        permission.setType(type);
        permission.setPath(r.path());
        permission.setIcon(r.icon());
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

    /**
     * 修改权限节点。
     *
     * <p>实现提示：按 id 查询并返回 404；检查 code 唯一、type/status 合法；父节点不能是自己，也不能是自己的后代，否则会形成环；更新允许修改的
     * 字段后保存。
     *
     * @param id 权限编号
     * @param request  新权限资料
     * @return 修改后的权限
     */
    @Override
    public SysPermission update(Long id, SystemRequests.PermissionSave request) {
        SysPermission permission = requiredPermission(id);
        validateRequest(request);
        String code=request.code().trim();
        String name=request.name().trim();
        String type=normalizeType(request.type());
        ensureCodeAvailable(code,id);
        if(Objects.equals(id,request.parentId())){
            throw BusinessException.badRequest("不能把自己设置为父权限");
        }
        ensureParentExists(request.parentId());
        String status = request.status()==null
                ?permission.getStatus()
                :normalizeStatus(request.status());
        int sortOrder=request.sortOrder()==null
                ?Objects.requireNonNullElse(permission.getSortOrder(),0)
                :request.sortOrder();
        permission.setParentId(request.parentId());
        permission.setCode(code);
        permission.setName(name);
        permission.setType(type);
        permission.setPath(request.path());
        permission.setIcon(request.icon());
        permission.setSortOrder(sortOrder);
        permission.setStatus(status);
        permission.setUpdatedAt(LocalDateTime.now());

        if (sysPermissionMapper.updateById(permission) != 1) {
            throw BusinessException.conflict("权限更新失败");
        }

        return permission;
    }

    /**
     * 删除权限节点。
     *
     * <p>实现提示（需要事务）：查询并返回 404；存在子节点时返回 409，要求先处理子节点；先清理 sys_role_permission 中对此权限的引用，再删除
     * 权限。当前认证过滤器会在每次请求重新装载权限，因此删除结果会在下一次请求生效。
     *
     * @param id 权限编号
     */
    @Override
    @Transactional
    public void delete(Long id) {
        requiredPermission(id);
        long childCount = sysPermissionMapper.countChildrenByParentId(id);
        if(childCount>0){
            throw BusinessException.conflict("该权限还有子权限，不能删除");
        }
        sysPermissionMapper.deleteRoleRelationsByPermissionId(id);
        if(sysPermissionMapper.deleteById(id)!=1){
            throw BusinessException.conflict("权限删除失败");
        }
    }

    private void validateRequest(SystemRequests.PermissionSave request){
        if(request==null
        ||!StringUtils.hasText(request.code())
        ||!StringUtils.hasText(request.name())
        ||!StringUtils.hasText(request.type())){
            throw BusinessException.badRequest("权限编码，名称和类型不能为空");
        }
    }

    private String normalizeType(String type){
        String normalized = type.trim().toUpperCase(Locale.ROOT);
        if(!PERMISSION_TYPES.contains(normalized)){
            throw BusinessException.badRequest("权限类型只能为MENU,BUTTON,或API");
        }
        return normalized;
    }

    private String statusForCreate(String status){
        if(!StringUtils.hasText(status)){
            return "ACTIVE";
        }
        return normalizeStatus(status);
    }

    private String normalizeStatus(String status){
        String normalized=status.trim().toUpperCase(Locale.ROOT);
        if(!PERMISSION_STATUSES.contains(normalized)){
            throw BusinessException.badRequest("权限状态只能为ACTIVE或者INACTIVE");
        }
        return normalized;
    }

    private void ensureCodeAvailable(String code,Long excludePermissionId){
        LambdaQueryWrapper<SysPermission> query = new LambdaQueryWrapper<SysPermission>().eq(SysPermission::getCode,code);
        if(excludePermissionId!=null){
            query.ne(SysPermission::getId,excludePermissionId);
        }
        if(sysPermissionMapper.selectOne(query)!=null){
            throw BusinessException.conflict("权限编码已经存在");
        }
    }

    private void ensureParentExists(Long parentId){
        if(parentId==null)return;
        if(sysPermissionMapper.selectById(parentId)==null){
            throw BusinessException.notFound("父权限"+parentId);
        }
    }

    private List<SysPermission>buildTree(List<SysPermission>permissions){
        Map<Long,SysPermission> permissionMap = new HashMap<>();
        for(SysPermission permission:permissions){
            permission.setChildren(new ArrayList<>());
            permissionMap.put(permission.getId(),permission);
        }
        List<SysPermission>roots=new ArrayList<>();
        for(SysPermission permission:permissions){
            Long parentId = permission.getParentId();
            SysPermission parent = permissionMap.get(parentId);
            if(parentId==null || parent==null){
                roots.add(permission);
            }else{
                parent.getChildren().add(permission);
            }
        }
        return roots;
    }

    private SysPermission requiredPermission(Long id){
        SysPermission permission = sysPermissionMapper.selectById(id);
        if(permission==null)throw BusinessException.notFound("权限");
        return permission;
    }
}
