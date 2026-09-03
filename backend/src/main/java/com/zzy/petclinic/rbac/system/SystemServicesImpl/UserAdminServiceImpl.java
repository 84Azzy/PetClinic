package com.zzy.petclinic.rbac.system.SystemServicesImpl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.system.*;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.HasAnnotation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
//@PreAuthorize("hasAuthority('medical:manage') && hasAnyRole('ADMIN', 'STAFF')")
@PreAuthorize("hasAuthority('system:manager') && hasAnyRole('ADMIN')")
public class UserAdminServiceImpl implements SystemServices.UserAdminService {

    private final SysUserAdminMapper sysUserAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleMapper sysRoleMapper;
    /**
     * 分页查询系统用户。
     *
     * <p>实现提示：计算 {@code offset = (pageValue - 1) * sizeValue}；分别调用
     * {@link SysUserAdminMapper#selectPageUsers(String, String, long, long)} 和
     * {@link SysUserAdminMapper#countUsers(String, String)}；最后组装 {@link PageResponse}。keyword 应同时匹配用户名、姓名、手机或
     * 邮箱，空白 keyword/status 不应拼接查询条件。
     *
     * <p><strong>安全要求：</strong>{@link SysUser} 当前含有密码哈希字段。正式接通接口前应改用不含密码的响应 DTO，或确保该字段不会被
     * Jackson 序列化，不能把哈希值发给前端。
     *
     * @param q 分页、关键词和状态条件
     * @return 用户分页数据
     */
    @Override
    public PageResponse<SysUser> page(PageQuery q) {
        long offset = (q.pageValue()-1)*q.sizeValue();
        List<SysUser> userList = sysUserAdminMapper.selectPageUsers(q.keyword(),q.status(),offset,q.sizeValue());
        long count = sysUserAdminMapper.countUsers(q.keyword(), q.status());
        return new PageResponse<>(userList,count,q.pageValue(),q.sizeValue());
    }

    /**
     * 新建用户。
     *
     * <p>实现提示：检查 username 未被占用；校验初始密码非空；使用 {@code PasswordEncoder.encode} 生成
     * {@code passwordHash}；复制姓名、电话、邮箱和账户类型；状态设为 ACTIVE、tokenVersion 设为 1；插入后返回。不要保存明文密码，
     * 重复用户名应转换成 409 冲突。
     *
     * @param r 已通过基础格式校验的用户资料
     * @return 新建用户（返回前移除密码哈希）
     */
    @Override
    public SysUser create(SystemRequests.UserSave r) {
        SysUser old = sysUserAdminMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername,r.username()));
        if(old!=null){
            throw BusinessException.conflict("该用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(r.username());
        user.setDisplayName(r.displayName());
        user.setPhone(r.phone());
        user.setEmail(r.email());
        user.setAccountType(r.accountType());
        user.setStatus("ACTIVE");
        LocalDateTime time = LocalDateTime.now();
        user.setCreatedAt(time);
        user.setUpdatedAt(time);
        String password = passwordEncoder.encode(r.password());
        user.setTokenVersion(1);
        user.setPasswordHash(password);
        sysUserAdminMapper.insert(user);
        return user;
    }

    /**
     * 修改用户基本资料。
     *
     * <p>实现提示：先按 id 查询，不存在返回 404；用户名变化时检查唯一性；更新 username、displayName、phone、email 和
     * accountType。普通编辑不要直接修改 passwordHash、status 或 tokenVersion，这些字段由专用方法维护。若允许修改用户名，应同步递增
     * tokenVersion，使旧 JWT 失效。
     *
     * @param id 用户编号
     * @param r  新的用户资料
     * @return 修改后的用户（返回前移除密码哈希）
     */
    @Override
    public SysUser update(Long id, SystemRequests.UserSave r) {
        SysUser user = sysUserAdminMapper.selectById(id);
        if(user==null){
            throw BusinessException.notFound("该用户不存在");
        }
        //todo 是使用wrapper.set还是直接user.set
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>();
        if(r.username()!=null){
            wrapper.set(SysUser::getUsername,r.username());
            wrapper.set(SysUser::getTokenVersion,user.getTokenVersion()+1);
            //todo JWT失效不是有redis才能做的吗
        }
        if(r.displayName()!=null){
           wrapper.set(SysUser::getDisplayName,r.displayName());
        }
        if(r.phone()!=null){
            wrapper.set(SysUser::getPhone,r.phone());
        }
        if(r.email()!=null){
            wrapper.set(SysUser::getEmail,r.email());
        }
        if(r.accountType()!=null){
            wrapper.set(SysUser::getAccountType,r.accountType());
        }
        wrapper.set(SysUser::getUpdatedAt,LocalDateTime.now());
        sysUserAdminMapper.update(wrapper);
        return user;
    }

    /**
     * 启用或停用用户。
     *
     * <p>实现提示：查询用户并返回 404；只接受 ACTIVE/INACTIVE；更新 status，同时把 tokenVersion 加一以撤销已签发 JWT；保存并检查
     * update 影响行数。应阻止管理员停用自己，避免把自己锁在系统外。
     *
     * @param id     用户编号
     * @param status 目标状态
     */
    @Override
    public void changeStatus(Long id, String status) {
        SysUser user = sysUserAdminMapper.selectById(id);
        if(user==null){
            throw BusinessException.notFound("该用户不存在");
        }
        if(!"ACTIVE".equals(status) || !"INACTIVE".equals(status)){
            throw BusinessException.conflict("状态非法");
        }
        String s = "ACTIVE".equals(status)? "INACTIVE" : "ACTIVE";
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(SysUser::getStatus,s);
        wrapper.set(SysUser::getTokenVersion,user.getTokenVersion()+1);
        sysUserAdminMapper.update(wrapper);
    }

    /**
     * 重置用户密码。
     *
     * <p>实现提示：查询用户并返回 404；再次校验密码长度；用 PasswordEncoder 哈希后写入 passwordHash；tokenVersion 加一，使旧 JWT
     * 立即失效；不要记录或返回明文密码。
     *
     * @param id       用户编号
     * @param password 新的明文密码，仅在本方法调用期间使用
     */
    @Override
    public void resetPassword(Long id, String password) {
        SysUser user = sysUserAdminMapper.selectById(id);
        if(user==null){
            throw BusinessException.notFound("该用户不存在");
        }
        //todo 校验正则不会写
        String s = passwordEncoder.encode(password);
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(SysUser::getPasswordHash,s);
        wrapper.set(SysUser::getTokenVersion,user.getTokenVersion()+1);
        //todo JWT过期
    }

    /**
     * 全量替换用户的角色。
     *
     * <p>实现提示（需要事务）：确认用户存在；对 ids 去重并拒绝 null；批量确认所有角色存在且状态为 ACTIVE；删除该用户原有
     * sys_user_role，再批量插入新关系；任一步失败都回滚。当前 JWT 过滤器会在每次请求时重新装载用户权限，因此新角色会在下一次请求生效；只有当
     * 产品要求角色变化后强制用户重新登录时，才额外递增 tokenVersion。
     *
     * @param id  用户编号
     * @param ids 完整的目标角色编号集合；空列表表示清空角色
     */
    @Override
    @Transactional
    public void assignRoles(Long id, List<Long> ids) {
        SysUser user = sysUserAdminMapper.selectById(id);
        if(user==null){
            throw BusinessException.notFound("该用户不存在");
        }
        List<Long> list = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<SysRole> roles = sysRoleMapper.selectByIds(list);
        List<Long>idList=roles.stream()
                .filter(role-> role.getStatus().equals("ACTIVE"))
                .map(SysRole::getId)
                .toList();
        //删除和新增
        sysUserAdminMapper.deleteRolesByUserId(id);
        sysUserAdminMapper.insertUserRoles(id,idList);
    }

    /**
     * 查询用户已有角色。
     *
     * <p>实现提示：先确认用户存在，再调用 {@link SysRoleMapper#selectByUserId(Long)}；结果按角色编码或 id 稳定排序。
     *
     * @param id 用户编号
     * @return 用户当前拥有的角色
     */
    @Override
    public List<SysRole> roles(Long id) {
        SysUser user = sysUserAdminMapper.selectById(id);
        if(user==null){
            throw BusinessException.notFound("该用户不存在");
        }
        List<SysRole> roles = sysRoleMapper.selectByUserId(id);
        return roles;
    }
}
