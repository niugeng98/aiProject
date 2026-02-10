package com.example.auth_demo.controller;

import com.example.auth_demo.common.Result;
import com.example.auth_demo.entity.User;
import com.example.auth_demo.service.LoginLogService;
import com.example.auth_demo.service.UserService;
import com.example.auth_demo.util.JwtUtil;
import com.example.auth_demo.config.RateLimiter;
import com.example.auth_demo.config.SecurityUtils;
import com.example.auth_demo.config.BlacklistManager;
import com.example.auth_demo.config.RiskAssessmentService;
import com.example.auth_demo.config.CaptchaService;
import com.example.auth_demo.entity.UsedWechatCode;
import com.example.auth_demo.mapper.UsedWechatCodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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
    @Autowired
    private RateLimiter rateLimiter;
    @Autowired
    private BlacklistManager blacklistManager;
    @Autowired
    private RiskAssessmentService riskAssessmentService;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private UsedWechatCodeMapper usedWechatCodeMapper;

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
        String deviceFingerprint = SecurityUtils.generateDeviceFingerprint(request);

        User existingUser = userService.findByUsername(username);
        if (existingUser == null || !passwordEncoder.matches(password, existingUser.getPassword())) {
            loginLogService.saveLoginLog(null, username, ipAddress, userAgent, "FAILURE", "Invalid username or password, Device: " + deviceFingerprint.substring(0, 8));
            return ResponseEntity.badRequest().body(Result.error(400, "用户名或密码错误"));
        }

        String token = jwtUtil.generateToken(existingUser.getId(), existingUser.getUsername());
        
        loginLogService.saveLoginLog(existingUser.getId(), username, ipAddress, userAgent, "SUCCESS", "Login successful, Device: " + deviceFingerprint.substring(0, 8));
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("userId", existingUser.getId());
        response.put("username", existingUser.getUsername());
        response.put("email", existingUser.getEmail());
        response.put("nickname", existingUser.getNickname());
        response.put("avatarUrl", existingUser.getAvatarUrl());
        response.put("phoneNumber", existingUser.getPhoneNumber());
        
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
    
    // 微信code有效期：5分钟
    private static final long WECHAT_CODE_EXPIRY = TimeUnit.MINUTES.toMillis(5);
    
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

    /**
     * 清理过期的微信code
     */
    private void cleanupExpiredWechatCodes() {
        QueryWrapper<UsedWechatCode> wrapper = new QueryWrapper<>();
        wrapper.lt("expiry_time", new Date());
        usedWechatCodeMapper.delete(wrapper);
    }
    
    @PostMapping("/wechat-login")
    public ResponseEntity<Result<?>> wechatLogin(@RequestBody Map<String, String> requestData, HttpServletRequest servletRequest) {
        try {
            // 1. 获取客户端IP地址
            String clientIp = SecurityUtils.getClientIp();
            
            // 2. 检查IP是否在黑名单中
            if (blacklistManager.isBlacklisted(clientIp)) {
                return ResponseEntity.badRequest().body(Result.error(403, "您的IP已被限制访问，请联系管理员"));
            }
            
            // 3. 生成设备指纹
            String deviceFingerprint = SecurityUtils.generateDeviceFingerprint(servletRequest);
            
            // 4. 检查速率限制（结合IP和设备指纹）
            if (!rateLimiter.isAllowed(clientIp, deviceFingerprint, "/api/users/wechat-login")) {
                // 连续请求过于频繁，返回429错误但不加入黑名单
                // 避免小程序前端因为网络波动等原因被误封
                return ResponseEntity.badRequest().body(Result.error(429, "请求过于频繁，请稍后再试"));
            }
            
            // 3. 获取 code 参数
            String code = requestData.get("code");
            if (code == null || code.isEmpty()) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: 未提供code"));
            }
            
            // 4. 检查微信code是否已经被使用过
            cleanupExpiredWechatCodes();
            QueryWrapper<UsedWechatCode> wrapper = new QueryWrapper<>();
            wrapper.eq("code", code);
            if (usedWechatCodeMapper.selectOne(wrapper) != null) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: code已被使用"));
            }
            
            // 4. 获取手机号 code
            String phoneCode = requestData.get("phoneCode");
            
            // 5. 微信小程序配置
            String appId = AppID;
            String appSecret = AppSecret;
            
            // 6. 调用微信接口验证 code
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
            
            // 7. 获取 openid
            String openid = wechatResponse.getStr("openid");
            if (openid == null) {
                return ResponseEntity.badRequest().body(Result.error(400, "微信登录失败: 未获取到openid"));
            }
            
            // 8. 获取手机号（如果有phoneCode）
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
            
            // 9. 查找或创建用户
            String username = openid;
            if (phoneNumber != null && SecurityUtils.isValidPhoneNumber(phoneNumber)) {
                // 使用手机号作为用户名
                username = phoneNumber;
            }
            
            User user = userService.findByUsername(username);
            if (user == null) {
                // 创建新用户
                user = new User();
                user.setUsername(username);
                // 使用随机密码，提高安全性
                String randomPassword = SecurityUtils.generateRandomPassword(16);
                user.setPassword(passwordEncoder.encode(randomPassword));
                user.setEmail(username + "@wechat.com");
                // 设置手机号
                if (phoneNumber != null && SecurityUtils.isValidPhoneNumber(phoneNumber)) {
                    user.setPhoneNumber(phoneNumber);
                }
                user = userService.register(user);
            }
            
            // 10. 生成 JWT token
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            // 11. 评估登录风险
            RiskAssessmentService.RiskLevel riskLevel = riskAssessmentService.assessLoginRisk(clientIp, deviceFingerprint, true);
            
            // 处理高风险登录
            if (riskLevel == RiskAssessmentService.RiskLevel.HIGH || riskLevel == RiskAssessmentService.RiskLevel.CRITICAL) {
                blacklistManager.addToBlacklist(clientIp);
                loginLogService.saveLoginLog(user.getId(), user.getUsername(), clientIp, "WeChat Mini Program", "FAILURE", "高风险登录行为，已被限制");
                return ResponseEntity.badRequest().body(Result.error(403, "您的登录行为存在风险，已被限制访问"));
            }
            
            // 处理中风险登录，要求验证码
            if (riskLevel == RiskAssessmentService.RiskLevel.MEDIUM) {
                String captchaKey = clientIp + "|" + deviceFingerprint;
                String captcha = requestData.get("captcha");
                
                if (captcha == null || captcha.isEmpty()) {
                    // 生成新验证码
                    String newCaptcha = captchaService.generateCaptcha(captchaKey);
                    // 这里应该返回验证码给前端，实际应用中应该返回验证码图片或发送到手机
                    // 为了演示，我们直接返回验证码（实际应用中不应该这样做）
                    Map<String, String> captchaResponse = new HashMap<>();
                    captchaResponse.put("captcha", newCaptcha);
                    captchaResponse.put("message", "请输入验证码以继续登录");
                    loginLogService.saveLoginLog(user.getId(), user.getUsername(), clientIp, "WeChat Mini Program", "PENDING", "中风险登录行为，需要验证码");
                    return ResponseEntity.ok(Result.success("需要验证码", captchaResponse));
                } else {
                    // 验证验证码
                    if (!captchaService.validateCaptcha(captchaKey, captcha)) {
                        loginLogService.saveLoginLog(user.getId(), user.getUsername(), clientIp, "WeChat Mini Program", "FAILURE", "验证码错误");
                        return ResponseEntity.badRequest().body(Result.error(400, "验证码错误，请重新输入"));
                    }
                }
            }
            
            // 12. 记录登录日志
            loginLogService.saveLoginLog(user.getId(), user.getUsername(), clientIp, "WeChat Mini Program", "SUCCESS", "WeChat login successful, Device: " + deviceFingerprint.substring(0, 8) + ", Risk Level: " + riskLevel);
            
            // 14. 标记微信code为已使用
            UsedWechatCode usedCode = new UsedWechatCode();
            usedCode.setCode(code);
            usedCode.setUsedTime(new Date());
            usedCode.setExpiryTime(new Date(System.currentTimeMillis() + WECHAT_CODE_EXPIRY));
            usedWechatCodeMapper.insert(usedCode);
            
            // 15. 返回 token 和用户信息
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("nickname", user.getNickname());
            response.put("avatarUrl", user.getAvatarUrl());
            response.put("phoneNumber", user.getPhoneNumber());
            
            return ResponseEntity.ok(Result.success("微信登录成功", response));
        } catch (Exception e) {
            e.printStackTrace();
            // 评估登录失败的风险
            try {
                String clientIp = SecurityUtils.getClientIp();
                String deviceFingerprint = SecurityUtils.generateDeviceFingerprint(servletRequest);
                RiskAssessmentService.RiskLevel riskLevel = riskAssessmentService.assessLoginRisk(clientIp, deviceFingerprint, false);
                
                // 记录失败日志
                loginLogService.saveLoginLog(null, null, clientIp, "WeChat Mini Program", "FAILURE", "微信登录失败: " + e.getMessage() + ", Risk Level: " + riskLevel);
                
                // 处理高风险失败
                if (riskLevel == RiskAssessmentService.RiskLevel.HIGH || riskLevel == RiskAssessmentService.RiskLevel.CRITICAL) {
                    blacklistManager.addToBlacklist(clientIp);
                }
            } catch (Exception ex) {
                // 忽略日志记录时的异常
            }
            
            return ResponseEntity.badRequest().body(Result.error(500, "微信登录失败: " + e.getMessage()));
        }
    }
}