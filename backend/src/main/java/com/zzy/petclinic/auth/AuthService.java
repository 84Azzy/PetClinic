package com.zzy.petclinic.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthAccountMapper mapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public LoginResponse login(LoginRequest request) {
    SysUser user =
        mapper.selectOne(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.username()));
    if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash()))
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
    if (!"ACTIVE".equals(user.getStatus()))
      throw new BusinessException(HttpStatus.FORBIDDEN, "账号已停用");
    return new LoginResponse(
        jwtService.issue(user), "Bearer", jwtService.expiresInSeconds(), profile(user));
  }

  public SysUser requireActive(Long id) {
    SysUser user = mapper.selectById(id);
    if (user == null || !"ACTIVE".equals(user.getStatus()))
      throw new BusinessException(HttpStatus.UNAUTHORIZED, "登录状态已失效");
    return user;
  }

  public UserProfile profile(SysUser user) {
    return new UserProfile(
        user.getId(),
        user.getUsername(),
        user.getDisplayName(),
        user.getPhone(),
        user.getEmail(),
        user.getAccountType());
  }
}
