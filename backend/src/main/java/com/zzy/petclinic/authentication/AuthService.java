package com.zzy.petclinic.authentication;

import com.zzy.petclinic.authentication.DTO.CurrentUserResponse;
import com.zzy.petclinic.authentication.DTO.LoginRequest;
import com.zzy.petclinic.authentication.DTO.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider tokenProvider;

  public LoginResponse login(LoginRequest request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
    return new LoginResponse(
        tokenProvider.generate(user),
        "Bearer",
        tokenProvider.expiresInSeconds(),
        CurrentUserResponse.from(user));
  }
}
