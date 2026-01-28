package com.example.auth_demo.controller;

import com.example.auth_demo.common.Result;
import com.example.auth_demo.entity.User;
import com.example.auth_demo.service.LoginLogService;
import com.example.auth_demo.service.UserService;
import com.example.auth_demo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private LoginLogService loginLogService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<Result<?>> register(@RequestBody User user) {
        if (userService.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.badRequest().body(Result.error(400, "用户名已存在"));
        }
        if (user.getEmail() != null && userService.findByEmail(user.getEmail()) != null) {
            return ResponseEntity.badRequest().body(Result.error(400, "邮箱已存在"));
        }
        User registeredUser = userService.register(user);
        return ResponseEntity.ok(Result.success("注册成功", null));
    }

    @PostMapping("/login")
    public ResponseEntity<Result<?>> login(@RequestBody User user, HttpServletRequest request) {
        String username = user.getUsername();
        String password = user.getPassword();
        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        User existingUser = userService.findByUsername(username);
        if (existingUser == null || !passwordEncoder.matches(password, existingUser.getPassword())) {
            loginLogService.saveLoginLog(null, username, ipAddress, userAgent, "FAILURE", "Invalid username or password");
            return ResponseEntity.badRequest().body(Result.error(400, "用户名或密码错误"));
        }

        String token = jwtUtil.generateToken(existingUser.getId(), existingUser.getUsername());
        
        loginLogService.saveLoginLog(existingUser.getId(), username, ipAddress, userAgent, "SUCCESS", "Login successful");
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", existingUser.getId());
        response.put("username", existingUser.getUsername());
        response.put("email", existingUser.getEmail());
        
        return ResponseEntity.ok(Result.success("登录成功", response));
    }

    @GetMapping("/profile")
    public ResponseEntity<Result<?>> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        User user = userService.findById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(Result.error(404, "用户不存在"));
        }
        // 不返回密码
        user.setPassword(null);
        return ResponseEntity.ok(Result.success(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<Result<?>> updateProfile(@RequestBody User user, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        User updatedUser = userService.updateProfile(userId, user);
        updatedUser.setPassword(null);
        return ResponseEntity.ok(Result.success("更新成功", updatedUser));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Result<?>> changePassword(@RequestBody Map<String, String> requestData, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.badRequest().body(Result.error(401, "未登录"));
        }
        String oldPassword = requestData.get("oldPassword");
        String newPassword = requestData.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Result.error(400, "参数不完整"));
        }
        
        User user = userService.findById(userId);
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Result.error(400, "原密码错误"));
        }
        
        userService.changePassword(userId, newPassword);
        return ResponseEntity.ok(Result.success("密码修改成功", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private final String AppID = "wx8d62ac19cf81b731";
    private final String AppSecret = "6a6676fb2b8fb75a3bd8f314513f9e1d";

    @PostMapping("/wechat-login")
    public ResponseEntity<Result<?>> wechatLogin(@RequestBody Map<String, String> requestData) {
        try {
            // 获取 code 参数
            String code = requestData.get("code");
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: 未提供code"));
            }
            
            // 1. 微信小程序配置
            String appId = AppID;
            String appSecret = AppSecret;
            
            // 2. 调用微信接口验证 code
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + appSecret + "&js_code=" + code + "&grant_type=authorization_code";
            
            // 使用 Java 原生 HTTP 客户端调用微信接口
            java.net.URL apiUrl = new java.net.URL(url);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) apiUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            
            // 读取响应（包括错误响应）
            int responseCode = connection.getResponseCode();
            java.io.BufferedReader reader;
            if (responseCode == 200) {
                reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
            } else {
                reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()));
            }
            StringBuilder responseBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
            reader.close();
            connection.disconnect();
            
            // 解析微信返回的 JSON
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> wechatResponse = mapper.readValue(responseBuilder.toString(), Map.class);
            
            // 检查是否有错误
            if (wechatResponse.containsKey("errcode")) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: " + wechatResponse.get("errmsg")));
            }
            
            // 获取 openid
            String openid = (String) wechatResponse.get("openid");
            if (openid == null) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: 未获取到openid"));
            }
            
            // 3. 查找或创建用户
            User user = userService.findByUsername(openid);
            if (user == null) {
                // 创建新用户
                user = new User();
                user.setUsername(openid);
                user.setPassword(passwordEncoder.encode(openid)); // 使用 openid 作为密码
                user.setEmail(openid + "@wechat.com");
                user = userService.register(user);
            }
            
            // 4. 生成 JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            // 5. 记录登录日志
            loginLogService.saveLoginLog(user.getId(), user.getUsername(), "127.0.0.1", "WeChat Mini Program", "SUCCESS", "WeChat login successful");
            
            // 6. 返回 token 和用户信息
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(Result.success("微信登录成功", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Result.error(500, "微信登录失败: " + e.getMessage()));
        }
    }
}