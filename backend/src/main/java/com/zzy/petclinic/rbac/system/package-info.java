/**
 * RBAC 后台管理模块，负责用户、角色、权限以及两张关联表的维护。
 *
 * <p>建议按 Mapper XML → Service 实现 → Controller 接线的顺序完成。涉及“先删除旧关系、再插入新关系”的分配操作必须由
 * Service 使用事务包裹；Controller 只做参数接收和响应包装，不承载业务规则。
 */
package com.zzy.petclinic.rbac.system;
