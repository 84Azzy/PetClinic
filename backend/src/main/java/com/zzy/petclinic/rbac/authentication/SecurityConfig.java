package com.zzy.petclinic.rbac.authentication;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity   //开启方法级注解
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  /**
   * 目标：
   * 把系统配置成基于JWT的无状态REST API
   * 登录注册等少数接口公开，其余接口必须携带有效jwt,
   * 更细的角色/权限判断交给@PreAuthorize
   * @param http
   * @return
   * @throws Exception
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())   //关闭跨站请求伪造，主要针对依靠cookie自动携带登录状态的系统
            //为啥需要接入cors，浏览器在正式发请求前会发预检请求，此时不携带JWT，防止直接401
            .cors(Customizer.withDefaults())  //启用spring security的cors支持，跨域规则定义在后面
        //无状态会话设置 SessionCreationPolicy.STATELESS不创建也不从session中恢复身份
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        //异常处理配置
        .exceptionHandling(
            exceptions ->
                exceptions
                    //401 没认证
                    .authenticationEntryPoint(authenticationEntryPoint)
                    //403 没权限
                    .accessDeniedHandler(accessDeniedHandler))
        //URL级授权规则
        .authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/health")
                    .permitAll()    //表示上述路径不需要用户已经认证，但还是会经过整条过滤链
                    .anyRequest()
                    .authenticated()) //anyRequest().authenticated()兜底规则，其他所有接口都需要认证
        //关闭spring security默认支持的表单登录，因为当前项目自己提供了登录controller
        .formLogin(form -> form.disable())
        //关闭http basic 因为当前项目使用 Authorization: 这个请求头来存放1jwt了
        .httpBasic(basic -> basic.disable())
        //todo 关键步骤 插入自定义jwt认证过滤链，并在UsernamePasswordAuthenticationFilter前执行
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
