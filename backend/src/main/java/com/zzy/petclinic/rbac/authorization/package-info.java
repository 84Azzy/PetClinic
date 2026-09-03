/**
 * 负责把 system 模块中的角色、权限数据转换成 Spring Security authority。
 *
 * <p>推荐实现顺序：先完成角色与权限 Mapper，再实现 {@code UserAuthorityService}，最后在业务接口上添加
 * {@code @PreAuthorize}。不要使用前端传入的账户类型或权限码做授权判断，服务端只信任认证上下文中装载的 authority。
 */
package com.zzy.petclinic.rbac.authorization;
