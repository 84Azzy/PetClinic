package com.zzy.petclinic.rbac.system;

import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.common.*;
import java.util.List;

/**
 * system 模块的业务契约集合。
 *
 * <p>这里故意只保留方法头，实际代码应分别放进 {@code UserAdminServiceImpl}、{@code RoleServiceImpl} 和
 * {@code PermissionServiceImpl}。实现类添加 {@code @Service}，用构造器注入 Mapper；涉及多次写库的方法添加
 * {@code @Transactional}。
 */
public final class SystemServices {
  private SystemServices() {}

  /** 用户账号管理业务；任何返回给前端的对象都不得包含 {@code passwordHash}。 */
  public interface UserAdminService {
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
    PageResponse<SysUser> page(PageQuery q);

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
    SysUser create(SystemRequests.UserSave r);

    /**
     * 修改用户基本资料。
     *
     * <p>实现提示：先按 id 查询，不存在返回 404；用户名变化时检查唯一性；更新 username、displayName、phone、email 和
     * accountType。普通编辑不要直接修改 passwordHash、status 或 tokenVersion，这些字段由专用方法维护。若允许修改用户名，应同步递增
     * tokenVersion，使旧 JWT 失效。
     *
     * @param id 用户编号
     * @param r 新的用户资料
     * @return 修改后的用户（返回前移除密码哈希）
     */
    SysUser update(Long id, SystemRequests.UserSave r);

    /**
     * 启用或停用用户。
     *
     * <p>实现提示：查询用户并返回 404；只接受 ACTIVE/INACTIVE；更新 status，同时把 tokenVersion 加一以撤销已签发 JWT；保存并检查
     * update 影响行数。应阻止管理员停用自己，避免把自己锁在系统外。
     *
     * @param id 用户编号
     * @param status 目标状态
     */
    void changeStatus(Long id, String status);

    /**
     * 重置用户密码。
     *
     * <p>实现提示：查询用户并返回 404；再次校验密码长度；用 PasswordEncoder 哈希后写入 passwordHash；tokenVersion 加一，使旧 JWT
     * 立即失效；不要记录或返回明文密码。
     *
     * @param id 用户编号
     * @param password 新的明文密码，仅在本方法调用期间使用
     */
    void resetPassword(Long id, String password);

    /**
     * 全量替换用户的角色。
     *
     * <p>实现提示（需要事务）：确认用户存在；对 ids 去重并拒绝 null；批量确认所有角色存在且状态为 ACTIVE；删除该用户原有
     * sys_user_role，再批量插入新关系；任一步失败都回滚。当前 JWT 过滤器会在每次请求时重新装载用户权限，因此新角色会在下一次请求生效；只有当
     * 产品要求角色变化后强制用户重新登录时，才额外递增 tokenVersion。
     *
     * @param id 用户编号
     * @param ids 完整的目标角色编号集合；空列表表示清空角色
     */
    void assignRoles(Long id, List<Long> ids);

    /**
     * 查询用户已有角色。
     *
     * <p>实现提示：先确认用户存在，再调用 {@link SysRoleMapper#selectByUserId(Long)}；结果按角色编码或 id 稳定排序。
     *
     * @param id 用户编号
     * @return 用户当前拥有的角色
     */
    List<SysRole> roles(Long id);
  }

  /** 角色及角色权限关系管理业务。 */
  public interface RoleService {
    /**
     * 查询全部角色。
     *
     * <p>实现提示：使用 MyBatis-Plus {@code selectList}，按 id 或 code 排序。后台管理页通常需要同时看到 ACTIVE 和 INACTIVE，不能默认只查
     * ACTIVE。
     *
     * @return 角色列表
     */
    List<SysRole> list();

    /**
     * 新建角色。
     *
     * <p>实现提示：检查 code 唯一；复制 code、name、description；status 为空时使用 ACTIVE，并只接受 ACTIVE/INACTIVE；插入后返回。
     * 并发下仍要把数据库唯一键异常转换成 409。
     *
     * @param r 角色资料
     * @return 新建角色
     */
    SysRole create(SystemRequests.RoleSave r);

    /**
     * 修改角色。
     *
     * <p>实现提示：按 id 查询并返回 404；code 变化时检查唯一性；校验状态值后更新允许修改的字段。若项目把 ADMIN 等内置角色视为保留角色，
     * 应禁止修改其 code。
     *
     * @param id 角色编号
     * @param r 新角色资料
     * @return 修改后的角色
     */
    SysRole update(Long id, SystemRequests.RoleSave r);

    /**
     * 删除角色。
     *
     * <p>实现提示（需要事务）：先确认角色存在；内置角色可直接禁止删除；若仍有用户绑定该角色，返回 409 并提示先解绑；清理
     * sys_role_permission 后再删除角色。不要依赖数据库外键异常作为正常业务流程。
     *
     * @param id 角色编号
     */
    void delete(Long id);

    /**
     * 全量替换角色拥有的权限。
     *
     * <p>实现提示（需要事务）：确认角色存在；对 ids 去重并拒绝 null；批量确认权限 id 全部存在；删除该角色原有
     * sys_role_permission，再批量插入新关系；空列表表示清空。当前 JWT 过滤器每次请求都会重新查权，所以变更会在下一次请求生效；不要在 JWT
     * 中复制一份长期不刷新的权限列表。
     *
     * @param id 角色编号
     * @param ids 完整的目标权限编号集合
     */
    void assignPermissions(Long id, List<Long> ids);
  }

  /** 权限节点管理业务。 */
  public interface PermissionService {
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
    List<SysPermission> tree();

    /**
     * 查询当前登录用户可用的权限树。
     *
     * <p>实现提示：从 {@code CurrentUser.id()} 获取用户编号，不接收前端 userId；调用
     * {@link SysPermissionMapper#selectByUserId(Long)} 得到已启用权限；再使用与 {@link #tree()} 相同的组树函数，避免复制递归逻辑。
     *
     * @return 当前用户拥有的权限树
     */
    List<SysPermission> mine();

    /**
     * 新建权限节点。
     *
     * <p>实现提示：检查 code 唯一；parentId 非空时确认父节点存在；type 只接受 MENU/BUTTON/API；复制 path、icon；sortOrder 为空用 0，
     * status 为空用 ACTIVE；插入后返回。并发重复 code 转换成 409。
     *
     * @param r 权限节点资料
     * @return 新建权限
     */
    SysPermission create(SystemRequests.PermissionSave r);

    /**
     * 修改权限节点。
     *
     * <p>实现提示：按 id 查询并返回 404；检查 code 唯一、type/status 合法；父节点不能是自己，也不能是自己的后代，否则会形成环；更新允许修改的
     * 字段后保存。
     *
     * @param id 权限编号
     * @param r 新权限资料
     * @return 修改后的权限
     */
    SysPermission update(Long id, SystemRequests.PermissionSave r);

    /**
     * 删除权限节点。
     *
     * <p>实现提示（需要事务）：查询并返回 404；存在子节点时返回 409，要求先处理子节点；先清理 sys_role_permission 中对此权限的引用，再删除
     * 权限。当前认证过滤器会在每次请求重新装载权限，因此删除结果会在下一次请求生效。
     *
     * @param id 权限编号
     */
    void delete(Long id);
  }
}
