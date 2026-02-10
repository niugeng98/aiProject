package com.example.auth_demo.config;

import com.example.auth_demo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 其他请求，正常处理token验证
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
                // 捕获token解析异常，确保即使token无效也能继续执行
                try {
                    username = jwtUtil.getUsernameFromToken(token);
                } catch (Exception e) {
                    // token解析失败，继续执行过滤器链
                    username = null;
                }
            }
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.validateToken(token)) {
                        // 将userId放入request attribute，方便控制器使用
                        try {
                            Long userId = jwtUtil.getUserIdFromToken(token);
                            if (userId != null) {
                                request.setAttribute("userId", userId);
                            }
                        } catch (Exception e) {
                            // userId解析失败，继续执行
                        }
                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                } catch (Exception e) {
                    // 用户加载或token验证失败，继续执行过滤器链
                }
            }
        } catch (Exception e) {
            // 捕获所有异常，确保过滤器链继续执行
            // 避免因为token问题而拦截其他请求
        }
        filterChain.doFilter(request, response);
    }
}