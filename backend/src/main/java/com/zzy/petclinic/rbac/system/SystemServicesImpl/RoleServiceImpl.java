package com.zzy.petclinic.rbac.system.SystemServicesImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.system.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.SslBundleSslEngineFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('system:manager')")
public class RoleServiceImpl implements SystemServices.RoleService {

    private static final Set<String> CODE = Set.of("ADMIN","STAFF","OWNER");
    private static final Set<String> STATUS = Set.of("ACTIVE","INACTIVE");

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
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByDesc(SysRole::getId,SysRole::getCode));
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
        validateCode(r.code());
        SysRole role = new SysRole();
        role.setCode(r.code());
        if(r.status()==null)role.setStatus("ACTIVE");
        else{
            validateStatus(r.status());
            role.setStatus(r.status());
        }
        validateRoleSave(r.name());
        role.setName(r.name());
        role.setDescription(r.description());
        try{
            if(sysRoleMapper.insert(role)!=1){
                throw BusinessException.conflict("角色插入失败，请重试");
            }
        } catch (DuplicateKeyException e) {
            throw BusinessException.conflict("角色已经存在");
        }
        LocalDateTime no = LocalDateTime.now();
        role.setCreatedAt(no);
        role.setUpdatedAt(no);
        return role;
    }

    /**
     * 修改角色。
     *
     * <p>实现提示：按 id 查询并返回 404；code 变化时检查唯一性；校验状态值后更新允许修改的字段。若项目把 ADMIN 等内置角色视为保留角色，
     * 应禁止修改其 code。
     *
     * @param id 角色编号
     * @param r  新角色资料
     * @return 修改后的角色
     */
    @Override
    public SysRole update(Long id, SystemRequests.RoleSave r) {
        SysRole role =requiredRole(id);
        if(CODE.contains(role.getCode())){
            throw BusinessException.badRequest("系统内置角色禁止修改");
        }
        //校验code唯一性
        if(r.code()!=null && !role.getCode().equals(r.code())){
            SysRole role1 = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getCode,r.code())
                    .ne(SysRole::getId,role.getId()));
            if(role1!=null){
                throw BusinessException.conflict("角色重复");
            }
            role.setCode(r.code());
        }
        if(StringUtils.hasText(r.status())){
            validateStatus(r.status());
            role.setStatus(r.status());
        }
        if(StringUtils.hasText(r.description())){
            role.setDescription(r.description());
        }
        role.setUpdatedAt(LocalDateTime.now());
        try{
            if(sysRoleMapper.updateById(role)!=1){
                throw BusinessException.conflict("角色更新失败，请重试");
            }
        }catch (DuplicateKeyException e){
            throw BusinessException.conflict("该角色已经存在");
        }
        return role;
    }

    /**
     * 删除角色。
     *
     * <p>实现提示（需要事务）：先确认角色存在；内置角色可直接禁止删除；若仍有用户绑定该角色，返回 409 并提示先解绑；清理
     * sys_role_permission 后再删除角色。不要依赖数据库外键异常作为正常业务流程。
     *
     * @param id 角色编号
     */
    @Override
    @Transactional
    public void delete(Long id) {
        SysRole role = requiredRole(id);
        if(CODE.contains(role.getCode())){
            throw BusinessException.badRequest("内置角色禁止删除");
        }
        if(sysRoleMapper.countUsersByRoleId(id)>0){
            throw BusinessException.badRequest("还有用户关联该角色，请先解除关系再删除");
        }
        sysRoleMapper.deletePermissionByRoleId(id);
        if(sysRoleMapper.deleteById(id)!=1){
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
     * @param id  角色编号
     * @param ids 完整的目标权限编号集合
     */
    @Override
    @Transactional
    public void assignPermissions(Long id, List<Long> ids) {
        SysRole role = requiredRole(id);
        if(ids==null || ids.stream().anyMatch(Objects::isNull)){
            throw BusinessException.badRequest("权限列表不能为空");
        }
        List<Long> permissionIds = List.copyOf(new LinkedHashSet<>(ids));
        if(!permissionIds.isEmpty()){
            Map<Long,SysPermission> permissionMap =
                    sysPermissionMapper.selectByIds(permissionIds).stream()
                            .collect(Collectors.toMap(SysPermission::getId, Function.identity()));
            for(Long permissionId : permissionIds){
                SysPermission permission = permissionMap.get(permissionId);
                if(permission==null){
                    throw BusinessException.notFound("权限"+permissionId);
                }
                if(!"ACTIVE".equals(permission.getStatus())){
                    throw BusinessException.conflict("权限"+permission.getCode()+"已停用");
                }
            }
        }
        sysRoleMapper.deletePermissionByRoleId(id);
        if(!permissionIds.isEmpty() && sysRoleMapper.insertRolePermissions(id,permissionIds)!=permissionIds.size()){
            throw BusinessException.conflict("权限分配失败，请重试");
        }
    }

    private SysRole requiredRole(Long id){
        SysRole role = sysRoleMapper.selectById(id);
        if(role==null){
            throw BusinessException.notFound("该角色不存在");
        }
        return role;
    }

    private void validateStatus(String s){
        if(s==null || !STATUS.contains(s)){
            throw BusinessException.badRequest("用户状态只能为ACTIVE或者INACTIVE");
        }
    }

    private void validateCode(String c){
        if(c==null || CODE.contains(c)){
            throw BusinessException.badRequest("角色code重复");
        }
    }

    private void validateRoleSave(String name){
        if(name==null ||name.isBlank()){
            throw BusinessException.badRequest("name字段不能为空");
        }
    }
}
