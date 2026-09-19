package com.zzy.petclinic.rbac.authentication;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerMapper;
import com.zzy.petclinic.rbac.authentication.DTO.CurrentUserResponse;
import com.zzy.petclinic.rbac.authentication.DTO.LoginRequest;
import com.zzy.petclinic.rbac.authentication.DTO.LoginResponse;
import com.zzy.petclinic.rbac.authentication.DTO.RegisterRequest;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {
  private static final String OWNER_ROLE_CODE = "OWNER";

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;
  private final SysUserMapper userMapper;
  private final OwnerMapper ownerMapper;
  private final PasswordEncoder passwordEncoder;

  public LoginResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
    return createLoginResponse(user);
  }

  @Transactional
  public LoginResponse register(RegisterRequest request) {
    if (!Objects.equals(request.password(), request.confirmPassword())) {
      throw BusinessException.badRequest("两次输入的密码不一致");
    }

    String username = request.username().trim();
    String displayName = request.displayName().trim();
    String phone = request.phone().trim();
    String email = normalizeOptional(request.email());
    String address = normalizeOptional(request.address());

    SysUser existingUser =
        userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
    if (existingUser != null) {
      throw BusinessException.conflict("该用户名已存在");
    }

    Long ownerRoleId = userMapper.selectActiveRoleIdByCode(OWNER_ROLE_CODE);
    if (ownerRoleId == null) {
      throw BusinessException.conflict("宠物主人角色不存在或已停用，暂时无法注册");
    }

    LocalDateTime now = LocalDateTime.now();
    SysUser user = new SysUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setDisplayName(displayName);
    user.setPhone(phone);
    user.setEmail(email);
    user.setAccountType("OWNER");
    user.setStatus("ACTIVE");
    user.setTokenVersion(1);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);

    try {
      if (userMapper.insert(user) != 1) {
        throw BusinessException.conflict("注册失败，请稍后重试");
      }
    } catch (DuplicateKeyException exception) {
      throw BusinessException.conflict("该用户名已存在");
    }

    if (userMapper.insertUserRole(user.getId(), ownerRoleId) != 1) {
      throw BusinessException.conflict("用户角色创建失败，请稍后重试");
    }

    Owner owner = new Owner();
    owner.setUserId(user.getId());
    owner.setName(displayName);
    owner.setPhone(phone);
    owner.setEmail(email);
    owner.setAddress(address);
    owner.setStatus("ACTIVE");
    owner.setCreatedAt(now);
    owner.setUpdatedAt(now);
    if (ownerMapper.insert(owner) != 1) {
      throw BusinessException.conflict("主人档案创建失败，请稍后重试");
    }

    return login(new LoginRequest(username, request.password()));
  }

  private LoginResponse createLoginResponse(AuthenticatedUser user) {
    return new LoginResponse(
        tokenProvider.generate(user),
        "Bearer",
        tokenProvider.expiresInSeconds(),
        CurrentUserResponse.from(user));
  }

  private String normalizeOptional(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
