package com.mz.finalcommunity.finalcommunity.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mz.finalcommunity.finalcommunity.dao.UserMapper;
import com.mz.finalcommunity.finalcommunity.entity.AccessToken;
import com.mz.finalcommunity.finalcommunity.entity.LoginTicket;
import com.mz.finalcommunity.finalcommunity.entity.User;
import com.mz.finalcommunity.finalcommunity.util.CommunityConstant;
import com.mz.finalcommunity.finalcommunity.util.CommunityUtil;
import com.mz.finalcommunity.finalcommunity.util.MailClient;
import com.mz.finalcommunity.finalcommunity.util.RedisKeyUtil;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    //@Autowired
    //private LoginTicketMapper loginTicketMapper;
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

    private User gitUser=new User();

    public User findUserById(int id) {
        //return userMapper.selectById(id);
        User user = getCache(id);
        if(user==null){
            user = initCache(id);
        }
        return user;
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        if (user == null) {
            throw new IllegalArgumentException("Not Null");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "username is null");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "password is null");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "Email is null");
            return map;
        }

        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "Username is exist");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "Email is exist");
            return map;
        }
        //register
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        //send email
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "Activate your account", content);
        return map;
    }

    public String bind(String email){
        if (StringUtils.isBlank(email)) {
            return null;
        }
        User byEmail = userMapper.selectByEmail(email);
        if(byEmail==null){
            gitUser.setEmail(email);
            gitUser.setStatus(0);
            gitUser.setActivationCode(CommunityUtil.generateUUID());
            userMapper.insertUser(gitUser);
            //send email
            Context context = new Context();
            context.setVariable("email", email);
            String url = domain + contextPath + "/activation/" + gitUser.getId() + "/" + gitUser.getActivationCode();
            context.setVariable("url", url);
            String content = templateEngine.process("/mail/activation", context);
            mailClient.sendMail(email, "Activate your account", content);
            return email;
        }else {
            return "1";
        }
    }

    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "uasername can not be null");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "password can not be null");
            return map;
        }
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "uasername can not be exist");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "uasername can not be Activate");
            return map;
        }
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "password Error");
            return map;
        }

        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {
        //loginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    //GitHub login
    public String getAccessToken(AccessToken accessToken) {
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(mediaType, JSON.toJSONString(accessToken));
        Request request = new Request.Builder()
                .url("https://github.com/login/oauth/access_token")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String string = response.body().string();
            String token = string.split("&")[0].split("=")[1];
            System.out.println("Access_Token is: " + token);
            return token;
        } catch (Exception e) {
            logger.error("GitHub Login failed:" + e.getMessage());
        }
        return null;
    }

    public Map<String, Object> getUser(String accessToken) {
        Map<String, Object> map = new HashMap<>();
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://api.github.com/user?access_token=" + accessToken)
                .build();
        LoginTicket loginTicket = new LoginTicket();
        try {
            Response response = client.newCall(request).execute();
            String string = response.body().string();
            JSONObject json = JSONObject.parseObject(string);
            User u = userMapper.selectByName(json.getString("login"));
            if (u == null) {
                gitUser.setUsername(json.getString("login"));
                gitUser.setHeaderUrl(json.getString("avatar_url"));
                gitUser.setEmail(json.getString("email"));
                gitUser.setType(0);
                gitUser.setStatus(0);
                gitUser.setCreateTime(new Date());
                String tes = gitUser.getEmail();
                System.out.println(tes);

                map.put("email", gitUser.getEmail());
                return map;
            }else {
                map.put("email",u.getEmail());
                if(map.get("email")!=null||map.get("email")!=""){
                    loginTicket.setUserId(u.getId());
                    loginTicket.setTicket(CommunityUtil.generateUUID());
                    loginTicket.setStatus(0);
                    loginTicket.setExpired(new Date(System.currentTimeMillis() + 3600 * 24 * 100 * 1000));
                    //loginTicketMapper.insertLoginTicket(loginTicket);
                    String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
                    redisTemplate.opsForValue().set(redisKey,loginTicket);
                    map.put("ticket", loginTicket.getTicket());
                }
                return map;
            }
        } catch (IOException e) { }
        return null;
    }
    public LoginTicket findLoginTicket(String ticket){
        //return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId,String headerUrl){
        //return userMapper.updateHeader(userId,headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    /**
     * Fetch data from the cache first
     * @param userId
     * @return
     */
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    /**
     * Initialize the cache when no data is retrieved from the cache
     * @param userId
     * @return
     */
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    /**
     * Clear cache when data changes
     * @param userId
     */
    private void clearCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<?extends GrantedAuthority> getAuthorities(int userId){
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
