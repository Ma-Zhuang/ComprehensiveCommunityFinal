package com.mz.finalcommunity.finalcommunity.controller.interceptor;

import com.mz.finalcommunity.finalcommunity.entity.LoginTicket;
import com.mz.finalcommunity.finalcommunity.entity.User;
import com.mz.finalcommunity.finalcommunity.service.UserService;
import com.mz.finalcommunity.finalcommunity.util.CookieUtil;
import com.mz.finalcommunity.finalcommunity.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //from cookie to get ticket
        String ticket = CookieUtil.getValue(request,"ticket");
        if(ticket!=null){
            //find ticket
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            //Check if the ticket are valid
            if(loginTicket!=null&&loginTicket.getStatus()==0&&loginTicket.getExpired().after(new Date())){
                //according the ticket find user
                User user = userService.findUserById(loginTicket.getUserId());
                hostHolder.setUserThreadLocal(user);
                //Construct the result of user authentication and store it in securityContext
                //User authorization for security
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(),userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User userThreadLocal = hostHolder.getUserThreadLocal();
        if(userThreadLocal!=null&&modelAndView!=null){
            modelAndView.addObject("loginUser",userThreadLocal);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
