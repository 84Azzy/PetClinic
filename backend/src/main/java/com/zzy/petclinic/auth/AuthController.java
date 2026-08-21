package com.zzy.petclinic.auth;

import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService service;
  private final CurrentUser currentUser;

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(service.login(request));
  }

  @GetMapping("/me")
  public ApiResponse<UserProfile> me() {
    return ApiResponse.ok(service.profile(service.requireActive(currentUser.id())));
  }
}
