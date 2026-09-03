package com.zzy.petclinic.rbac.authentication;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.rbac.authorization.UserAuthorityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {
  private final SysUserMapper userMapper;
  private final UserAuthorityService userAuthorityService;

  @Override
  public UserDetails loadUserByUsername(String username) {
    SysUser user =
        userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    if (user == null) throw new UsernameNotFoundException("用户名或密码错误");

    UserAuthorities authorities = userAuthorityService.loadByUserId(user.getId());
    return new AuthenticatedUser(
        user, authorities == null ? UserAuthorities.empty() : authorities);
  }
}
