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
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

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
    
    // 获取微信接口调用凭证
    private String getAccessToken(String appId, String appSecret) throws Exception {
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        
        // 使用HuTool的HttpRequest调用接口
        String responseStr = HttpRequest.get(url)
                .timeout(5000)
                .execute()
                .body();
        
        // 使用HuTool的JSONUtil解析JSON
        JSONObject response = JSONUtil.parseObj(responseStr);
        
        if (response.containsKey("errcode") && !"0".equals(response.getStr("errcode"))) {
            throw new Exception("获取access_token失败: " + response.getStr("errmsg"));
        }
        
        return response.getStr("access_token");
    }

    @PostMapping("/wechat-login")
    public ResponseEntity<Result<?>> wechatLogin(@RequestBody Map<String, String> requestData) {
        try {
            // 获取 code 参数
            String code = requestData.get("code");
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: 未提供code"));
            }
            
            // 获取手机号 code
            String phoneCode = requestData.get("phoneCode");
            
            // 1. 微信小程序配置
            String appId = AppID;
            String appSecret = AppSecret;
            
            // 2. 调用微信接口验证 code
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + appSecret + "&js_code=" + code + "&grant_type=authorization_code";
            
            // 使用HuTool的HttpRequest调用接口
            String responseStr = HttpRequest.get(url)
                    .timeout(5000)
                    .execute()
                    .body();
            
            // 使用HuTool的JSONUtil解析JSON
            JSONObject wechatResponse = JSONUtil.parseObj(responseStr);
            
            // 检查是否有错误
            if (wechatResponse.containsKey("errcode")) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: " + wechatResponse.getStr("errmsg")));
            }
            
            // 获取 openid
            String openid = wechatResponse.getStr("openid");
            if (openid == null) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: 未获取到openid"));
            }
            
            // 3. 获取手机号（如果有phoneCode）
            String phoneNumber = null;
            if (phoneCode != null && !phoneCode.isEmpty()) {
                // 调用微信接口获取手机号
                String phoneUrl = "https://api.weixin.qq.com/cgi-bin/wxa/business/getuserphonenumber?access_token=" + getAccessToken(appId, appSecret);
                
                // 构建请求体
                JSONObject requestBody = new JSONObject();
                requestBody.put("code", phoneCode);
                
                // 使用HuTool的HttpRequest调用接口
                String phoneResponseStr = HttpRequest.post(phoneUrl)
                        .timeout(5000)
                        .contentType("application/json")
                        .body(requestBody.toString())
                        .execute()
                        .body();
                
                // 使用HuTool的JSONUtil解析JSON
                JSONObject phoneWechatResponse = JSONUtil.parseObj(phoneResponseStr);
                if (phoneWechatResponse.containsKey("errcode") && !"0".equals(phoneWechatResponse.getStr("errcode"))) {
                    return ResponseEntity.badRequest().body(Result.error(400, "获取手机号失败: " + phoneWechatResponse.getStr("errmsg")));
                }
                
                // 提取手机号
                JSONObject phoneInfo = phoneWechatResponse.getJSONObject("phone_info");
                if (phoneInfo != null) {
                    phoneNumber = phoneInfo.getStr("phoneNumber");
                }
            }
            
            // 4. 查找或创建用户
            String username = openid;
            if (phoneNumber != null) {
                // 使用手机号作为用户名
                username = phoneNumber;
            }
            
            User user = userService.findByUsername(username);
            if (user == null) {
                // 创建新用户
                user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(username)); // 使用用户名作为密码
                user.setEmail(username + "@wechat.com");
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