package com.agent.api;

import com.agent.auth.AuthService;
import com.agent.auth.LoginResult;
import com.agent.auth.PlatformRole;
import com.agent.auth.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 注册和登录入口；注册用户默认只有 USER 角色，不接受客户端自行提权。 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisteredUserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserAccount account = authService.register(request.username(), request.password(), request.tenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(RegisteredUserResponse.from(account)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.of(LoginResponse.from(authService.login(request.username(), request.password())));
    }

    public record RegisterRequest(@NotBlank @Size(min = 3, max = 64) String username,
                                  @NotBlank @Size(min = 8, max = 128) String password,
                                  @NotBlank @Size(max = 128) String tenantId) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record RegisteredUserResponse(String id, String username, String tenantId, Set<PlatformRole> roles, Instant createdAt) {
        static RegisteredUserResponse from(UserAccount account) {
            return new RegisteredUserResponse(account.id(), account.username(), account.tenantId(), account.roles(), account.createdAt());
        }
    }

    public record LoginResponse(String accessToken, String tokenType, Instant expiresAt, String username,
                                String tenantId, Set<PlatformRole> roles) {
        static LoginResponse from(LoginResult result) {
            return new LoginResponse(result.accessToken(), "Bearer", result.expiresAt(), result.username(), result.tenantId(), result.roles());
        }
    }
}
