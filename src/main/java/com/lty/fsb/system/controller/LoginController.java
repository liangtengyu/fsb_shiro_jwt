package com.lty.fsb.system.controller;

import com.alibaba.fastjson.JSON;
import com.lty.fsb.common.annotation.Limit;
import com.lty.fsb.common.authentication.JWTToken;
import com.lty.fsb.common.authentication.JWTUtil;
import com.lty.fsb.common.domain.ActiveUser;
import com.lty.fsb.common.domain.FsbConstant;
import com.lty.fsb.common.domain.FsbResponse;
import com.lty.fsb.common.exception.FsbGlobalException;
import com.lty.fsb.common.exception.RedisConnectException;
import com.lty.fsb.common.properties.FsbProperties;
import com.lty.fsb.common.service.RedisService;
import com.lty.fsb.common.utils.sms.request.SmsSendRequest;
import com.lty.fsb.common.utils.sms.util.ChuangLanSmsUtil;
import com.lty.fsb.system.dao.LoginLogMapper;
import com.lty.fsb.system.domain.LoginLog;
import com.lty.fsb.system.domain.User;
import com.lty.fsb.system.domain.UserConfig;
import com.lty.fsb.system.manager.UserManager;
import com.lty.fsb.system.service.LoginLogService;
import com.lty.fsb.system.service.UserService;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lty.fsb.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.*;

@Validated
@RestController
@Slf4j
public class LoginController {

    @Autowired
    private RedisService redisService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private UserService userService;
    @Autowired
    private LoginLogService loginLogService;
    @Autowired
    private LoginLogMapper loginLogMapper;
    @Autowired
    private FsbProperties properties;
    @Autowired
    private ObjectMapper mapper;

    @PostMapping("/login")
    @Limit(key = "login", period = 60, count = 20, name = "登录接口", prefix = "limit")
    public FsbResponse login(
            @NotBlank(message = "{required}") String username,
            @NotBlank(message = "{required}") String password, HttpServletRequest request) throws Exception {
        username = StringUtils.lowerCase(username);
        password = MD5Util.encrypt(username, password);

        final String errorMessage = "用户名或密码错误";
        User user = this.userManager.getUser(username);

        if (user == null)
            throw new FsbGlobalException(errorMessage);
        if (!StringUtils.equals(user.getPassword(), password))
            throw new FsbGlobalException(errorMessage);
        if (User.STATUS_LOCK.equals(user.getStatus()))
            throw new FsbGlobalException("账号已被锁定,请联系管理员！");

        // 更新用户登录时间
        this.userService.updateLoginTime(username);
        // 保存登录记录
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        this.loginLogService.saveLoginLog(loginLog);

        String token = MyUtil.encryptToken(JWTUtil.sign(username, password));
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(properties.getShiro().getJwtTimeOut());
        String expireTimeStr = DateUtil.formatFullTime(expireTime);
        JWTToken jwtToken = new JWTToken(token, expireTimeStr);

        String userId = this.saveTokenToRedis(user, jwtToken, request);
        user.setId(userId);

        Map<String, Object> userInfo = this.generateUserInfo(jwtToken, user);
        return new FsbResponse().message("认证成功").data(userInfo);
    }

    @GetMapping("index/{username}")
    public FsbResponse index(@NotBlank(message = "{required}") @PathVariable String username) {
        Map<String, Object> data = new HashMap<>();
        // 获取系统访问记录
        Long totalVisitCount = loginLogMapper.findTotalVisitCount();
        data.put("totalVisitCount", totalVisitCount);
        Long todayVisitCount = loginLogMapper.findTodayVisitCount();
        data.put("todayVisitCount", todayVisitCount);
        Long todayIp = loginLogMapper.findTodayIp();
        data.put("todayIp", todayIp);
        // 获取近期系统访问记录
        List<Map<String, Object>> lastSevenVisitCount = loginLogMapper.findLastSevenDaysVisitCount(null);
        data.put("lastSevenVisitCount", lastSevenVisitCount);
        User param = new User();
        param.setUsername(username);
        List<Map<String, Object>> lastSevenUserVisitCount = loginLogMapper.findLastSevenDaysVisitCount(param);
        data.put("lastSevenUserVisitCount", lastSevenUserVisitCount);
        return new FsbResponse().data(data);
    }

    @RequiresPermissions("user:online")
    @GetMapping("online")
    public FsbResponse userOnline(String username) throws Exception {
        String now = DateUtil.formatFullTime(LocalDateTime.now());
        Set<String> userOnlineStringSet = redisService.zrangeByScore(FsbConstant.ACTIVE_USERS_ZSET_PREFIX, now, "+inf");
        List<ActiveUser> activeUsers = new ArrayList<>();
        for (String userOnlineString : userOnlineStringSet) {
            ActiveUser activeUser = mapper.readValue(userOnlineString, ActiveUser.class);
            activeUser.setToken(null);
            if (StringUtils.isNotBlank(username)) {
                if (StringUtils.equalsIgnoreCase(username, activeUser.getUsername()))
                    activeUsers.add(activeUser);
            } else {
                activeUsers.add(activeUser);
            }
        }
        return new FsbResponse().data(activeUsers);
    }

    @DeleteMapping("kickout/{id}")
    @RequiresPermissions("user:kickout")
    public void kickout(@NotBlank(message = "{required}") @PathVariable String id) throws Exception {
        String now = DateUtil.formatFullTime(LocalDateTime.now());
        Set<String> userOnlineStringSet = redisService.zrangeByScore(FsbConstant.ACTIVE_USERS_ZSET_PREFIX, now, "+inf");
        ActiveUser kickoutUser = null;
        String kickoutUserString = "";
        for (String userOnlineString : userOnlineStringSet) {
            ActiveUser activeUser = mapper.readValue(userOnlineString, ActiveUser.class);
            if (StringUtils.equals(activeUser.getId(), id)) {
                kickoutUser = activeUser;
                kickoutUserString = userOnlineString;
            }
        }
        if (kickoutUser != null && StringUtils.isNotBlank(kickoutUserString)) {
            // 删除 zset中的记录
            redisService.zrem(FsbConstant.ACTIVE_USERS_ZSET_PREFIX, kickoutUserString);
            // 删除对应的 token缓存
            redisService.del(FsbConstant.TOKEN_CACHE_PREFIX + kickoutUser.getToken() + "." + kickoutUser.getIp());
        }
    }

    @GetMapping("logout/{id}")
    public void logout(@NotBlank(message = "{required}") @PathVariable String id) throws Exception {
        this.kickout(id);
    }

    @PostMapping("regist")
    public FsbResponse regist(
            @NotBlank(message = "{required}") String username,
            @NotBlank(message = "{required}") String phoneNum,
            @NotBlank(message = "{required}") String verifyCode,
            @NotBlank(message = "{required}") String password) throws Exception {
        return userService.regist(username, password,phoneNum,verifyCode);
    }

    private String saveTokenToRedis(User user, JWTToken token, HttpServletRequest request) throws Exception {
        String ip = IPUtil.getIpAddr(request);

        // 构建在线用户
        ActiveUser activeUser = new ActiveUser();
        activeUser.setUsername(user.getUsername());
        activeUser.setIp(ip);
        activeUser.setToken(token.getToken());
        activeUser.setLoginAddress(AddressUtil.getCityInfo(ip));

        // zset 存储登录用户，score 为过期时间戳
        this.redisService.zadd(FsbConstant.ACTIVE_USERS_ZSET_PREFIX, Double.valueOf(token.getExipreAt()), mapper.writeValueAsString(activeUser));
        // redis 中存储这个加密 token，key = 前缀 + 加密 token + .ip
        this.redisService.set(FsbConstant.TOKEN_CACHE_PREFIX + token.getToken() + StringPool.DOT + ip, token.getToken(), properties.getShiro().getJwtTimeOut() * 1000);

        return activeUser.getId();
    }

    /**
     * 生成前端需要的用户信息，包括：
     * 1. token
     * 2. Vue Router
     * 3. 用户角色
     * 4. 用户权限
     * 5. 前端系统个性化配置信息
     *
     * @param token token
     * @param user  用户信息
     * @return UserInfo
     */
    private Map<String, Object> generateUserInfo(JWTToken token, User user) {
        String username = user.getUsername();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("token", token.getToken());
        userInfo.put("exipreTime", token.getExipreAt());

        Set<String> roles = this.userManager.getUserRoles(username);
        userInfo.put("roles", roles);

        Set<String> permissions = this.userManager.getUserPermissions(username);
        userInfo.put("permissions", permissions);

        UserConfig userConfig = this.userManager.getUserConfig(String.valueOf(user.getUserId()));
        userInfo.put("config", userConfig);

        user.setPassword("it's a secret");
        userInfo.put("user", user);
        return userInfo;
    }

    @Autowired
    Config config;

    @PostMapping("getCaptcha")
    public FsbResponse getCaptcha(String phoneNum) throws RedisConnectException {
        StringBuffer b = new StringBuffer();
        for (int i = 0; i < 5; i++) {
            int number=(int)(Math.random()*10);
            b.append(number);
        }
        //String code = "11111";
        String code = b.toString();
        b.insert(0, "你的注册码为: ");
        SmsSendRequest smsSingleRequest = new SmsSendRequest(
                config.getChuanglan_account(), config.getChuanglan_password(),
                b.toString(), phoneNum,"true");
        log.info("发送短信:{} 给 {}", b.toString(), phoneNum);
        String requestJson = JSON.toJSONString(smsSingleRequest);

        String response = ChuangLanSmsUtil.sendSmsByPost(config.getChuanglan_url(), requestJson);

        redisService.set("verify-"+phoneNum + "-" + code, code,1000L*180L);
        return new FsbResponse().put("msgCode", 00000);

    }

}
