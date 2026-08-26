package com.zzy.petclinic.authentication;

import com.zzy.petclinic.authentication.DTO.CurrentUserResponse;
import com.zzy.petclinic.authentication.DTO.LoginRequest;
import com.zzy.petclinic.authentication.DTO.LoginResponse;
import com.zzy.petclinic.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;
  private final CurrentUser currentUser;

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.ok(authService.login(request));
  }

  @GetMapping("/me")
  public ApiResponse<CurrentUserResponse> me() {
    return ApiResponse.ok(CurrentUserResponse.from(currentUser.require()));
  }
}
