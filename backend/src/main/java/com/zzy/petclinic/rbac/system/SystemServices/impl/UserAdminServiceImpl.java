package com.zzy.petclinic.rbac.system.SystemServices.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.rbac.authentication.SysUser;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.zzy.petclinic.rbac.system.SystemServices.SystemServices;
import com.zzy.petclinic.rbac.system.dataObject.SysRole;
import com.zzy.petclinic.rbac.system.dataObject.SystemRequests;
import com.zzy.petclinic.rbac.system.mapper.SysRoleMapper;
import com.zzy.petclinic.rbac.system.mapper.SysUserAdminMapper;
import lombok.RequiredArgsConstructor;
//DuplicateKeyException Spring 的持久层异常：唯一索引 / 主键重复异常
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
// 不恰当：数据库和 Controller 使用的权限码是 system:manage，system:manager 会让管理员也无法调用。
// @PreAuthorize("hasAuthority('system:manager') && hasAnyRole('ADMIN')")
@PreAuthorize("hasAuthority('system:manage')")
public class UserAdminServiceImpl implements SystemServices.UserAdminService {

    private static final Set<String> ACCOUNT_TYPES = Set.of("ADMIN", "STAFF", "OWNER");
    private static final Set<String> USER_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final SysUserAdminMapper sysUserAdminMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleMapper sysRoleMapper;
    private final CurrentUser currentUser;
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
        // 不恰当：直接透传空白筛选值，XML 容易生成多余条件；分页与计数也重复读取参数。
        // List<SysUser> users = sysUserAdminMapper.selectPageUsers(
        //         q.keyword(), q.status(), (q.pageValue() - 1) * q.sizeValue(), q.sizeValue());
        String keyword = StringUtils.hasText(q.keyword()) ? q.keyword().trim() : null;
        String status = StringUtils.hasText(q.status()) ? q.status().trim() : null;
        long page = q.pageValue();
        long size = q.sizeValue();
        long offset = (page - 1) * size;
        List<SysUser> users =
                sysUserAdminMapper.selectPageUsers(keyword, status, offset, size);
        long total = sysUserAdminMapper.countUsers(keyword, status);
        return new PageResponse<>(users, total, page, size);
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
        //校验参数
        validateUserSave(r);
        validateAccountType(r.accountType());
        validatePassword(r.password());

        SysUser old =
                sysUserAdminMapper.selectOne(
                        new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, r.username()));
        if (old != null) {
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
        user.setTokenVersion(1);
        user.setPasswordHash(passwordEncoder.encode(r.password()));
        // 不恰当：忽略 insert 影响行数，并把带 passwordHash 的实体直接返回。
        // sysUserAdminMapper.insert(user);
        // return user;
        try {
            if (sysUserAdminMapper.insert(user) != 1) {
                throw BusinessException.conflict("用户创建失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("该用户名已存在");
        }
        //不把passwordHash发给前端
        user.setPasswordHash(null);
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
        //根据id查用户
        SysUser user = requiredUser(id);

        validateUserSave(r);
        validateAccountType(r.accountType());

        /*
         * 不恰当：原 LambdaUpdateWrapper 没有 eq(id)，update(wrapper) 可能更新整张 sys_user；
         * 同时返回的 user 从未同步新字段。
         * LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
         * wrapper.set(SysUser::getUsername, r.username());
         * sysUserAdminMapper.update(wrapper);
         * return user;
         */
        if (!Objects.equals(user.getUsername(), r.username())) {
            //判断是否已有r.username这个用户名的用户
            SysUser sameName =
                    sysUserAdminMapper.selectOne(
                            new LambdaQueryWrapper<SysUser>()
                                    .eq(SysUser::getUsername, r.username())
                                    .ne(SysUser::getId, id));
            if (sameName != null) {
                throw BusinessException.conflict("该用户名已存在");
            }
            user.setUsername(r.username());
            // 无需 Redis：JWT 过滤器每次都会重新查用户，并比较 tokenVersion。
            user.setTokenVersion(nextTokenVersion(user));
        }
        user.setDisplayName(r.displayName());
        user.setPhone(r.phone());
        user.setEmail(r.email());
        user.setAccountType(r.accountType());
        user.setUpdatedAt(LocalDateTime.now());
        try {
            if (sysUserAdminMapper.updateById(user) != 1) {
                throw BusinessException.conflict("用户资料更新失败，请重试");
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.conflict("该用户名已存在");
        }
        user.setPasswordHash(null);
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
        SysUser user = requiredUser(id);
        /*
         * 不恰当：使用 || 会让 ACTIVE/INACTIVE 都判为非法；之后还把“设置状态”写成了“切换状态”。
         * if (!"ACTIVE".equals(status) || !"INACTIVE".equals(status)) { ... }
         * String target = "ACTIVE".equals(status) ? "INACTIVE" : "ACTIVE";
         */
        validateStatus(status);
        if (Objects.equals(currentUser.id(), id) && "INACTIVE".equals(status)) {
            throw BusinessException.conflict("不能停用当前登录账号");
        }
        if (Objects.equals(user.getStatus(), status)) {
            return;
        }
        user.setStatus(status);
        user.setTokenVersion(nextTokenVersion(user));
        user.setUpdatedAt(LocalDateTime.now());
        if (sysUserAdminMapper.updateById(user) != 1) {
            throw BusinessException.conflict("用户状态更新失败，请重试");
        }
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
        SysUser user = requiredUser(id);
        validatePassword(password);
        /*
         * 不恰当：原代码只构造 wrapper，没有执行 Mapper 更新，因此密码和 tokenVersion 都不会落库。
         * LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
         * wrapper.set(SysUser::getPasswordHash, passwordEncoder.encode(password));
         */
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTokenVersion(nextTokenVersion(user));
        user.setUpdatedAt(LocalDateTime.now());
        if (sysUserAdminMapper.updateById(user) != 1) {
            throw BusinessException.conflict("密码重置失败，请重试");
        }
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
        requiredUser(id);
        //ids只要有元素有null，全都拒了
        if (ids == null || ids.stream().anyMatch(Objects::isNull)) {
            throw badRequest("角色编号不能为空");
        }
        List<Long> roleIds = List.copyOf(new LinkedHashSet<>(ids));

        /*
         * 不恰当：原实现过滤掉 null、停用角色和不存在角色后继续保存，调用者会误以为全部分配成功。
         * List<Long> idList = roles.stream().filter(role -> role.getStatus().equals("ACTIVE"))...
         */
        if (!roleIds.isEmpty()) {
            Map<Long, SysRole> rolesById =
                    sysRoleMapper.selectByIds(roleIds).stream()
                            //`Function.identity()` = 返回流中当前遍历的元素本身；在当前上下文中表示遍历的sysRole对象
                            //map里面的值为roleId:sysRole
                            .collect(Collectors.toMap(SysRole::getId, Function.identity()));
            for (Long roleId : roleIds) {
                SysRole role = rolesById.get(roleId);
                if (role == null) {
                    throw BusinessException.notFound("角色 " + roleId);
                }
                if (!"ACTIVE".equals(role.getStatus())) {
                    throw BusinessException.conflict("角色 " + roleId + " 已停用");
                }
            }
        }

        sysUserAdminMapper.deleteRolesByUserId(id);
        if (!roleIds.isEmpty()
                && sysUserAdminMapper.insertUserRoles(id, roleIds) != roleIds.size()) {
            throw BusinessException.conflict("用户角色分配失败，请重试");
        }
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
        // 不恰当：查询后再创建同义局部变量，没有增加任何校验或转换。
        // List<SysRole> roles = sysRoleMapper.selectByUserId(id);
        // return roles;
        requiredUser(id);
        return sysRoleMapper.selectByUserId(id);
    }

    private SysUser requiredUser(Long id) {
        SysUser user = sysUserAdminMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户");
        }
        return user;
    }

    private void validateAccountType(String accountType) {
        if (accountType == null || !ACCOUNT_TYPES.contains(accountType)) {
            throw badRequest("账号类型只允许 ADMIN、STAFF 或 OWNER");
        }
    }

    private void validateStatus(String status) {
        if (status == null || !USER_STATUSES.contains(status)) {
            throw badRequest("用户状态只允许 ACTIVE 或 INACTIVE");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6) {
            throw badRequest("密码长度不能少于 6 位");
        }
    }

    private void validateUserSave(SystemRequests.UserSave request) {
        if (request == null
                || !StringUtils.hasText(request.username())
                || !StringUtils.hasText(request.displayName())) {
            throw badRequest("用户名和姓名不能为空");
        }
    }

    private int nextTokenVersion(SysUser user) {
        return (user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
}
