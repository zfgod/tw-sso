package com.tongwei.sso.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tongwei.Const;
import com.tongwei.auth.model.User;
import com.tongwei.auth.security.RememberMeType;
import com.tongwei.auth.security.rule.RememberMeRule;
import com.tongwei.auth.util.SessionUtil;
import com.tongwei.common.BaseController;
import com.tongwei.common.exception.AuthenticationExcption;
import com.tongwei.common.model.Result;
import com.tongwei.common.util.ResultUtil;
import com.tongwei.sso.service.IUserService;
import com.tongwei.sso.util.AuthService;

@Controller
@ConfigurationProperties(prefix="sso.sys")
public class LoginController extends BaseController {
	
	private String successUrl;
	
	@Autowired
	IUserService userService;
	
	@Autowired
	AuthService authService;
	
	@Autowired
	RememberMeRule rememberMeRule;
	
	
	@PostMapping("/login")
	@ResponseBody
    public Result login(String loginName,String password,String successUrl,String rememberMe,String rememberMeType) throws Exception{
    	if(StringUtils.isBlank(loginName) || StringUtils.isBlank(password)){
    		return ResultUtil.doFailure("用户名或密码不能为空!");
    	}
    	User user =null;
    	try {
    		user = authService.login(loginName,password);
		} catch (AuthenticationExcption e) {
			return ResultUtil.doFailure(e.getMessage());
		}
    	SessionUtil.setUser(user);//登录成功
    	
    	if("null".equals(successUrl) || StringUtils.isBlank(successUrl)){
    		successUrl = this.successUrl;
    	}
    	
    	Map<String, String> data = new HashMap<>(2);
    	
    	Cookie[] cookies = request.getCookies();
    	if(cookies!=null){
    		for (Cookie cookie : cookies) {
    			String name = cookie.getName();
    			String value = cookie.getValue();
				if("SESSION".equalsIgnoreCase(name)){
					data.put("SESSION", value);
				}
				if("AUTHUSER".equalsIgnoreCase(name)){
					data.put("AUTHUSER", value);
				}
			}
    	}
    	
    	//记住我
    	if("on".equals(rememberMe)){
    		RememberMeType re = RememberMeType.USER_AGENT;
    		if("HOST".equalsIgnoreCase(rememberMeType)){
    			re = RememberMeType.HOST;
    		}
    		if("NONE".equalsIgnoreCase(rememberMeType)){
    			re = RememberMeType.NONE;
    		}
    		String generateValue = rememberMeRule.generateValue(request, re, user.getId());
    		if(generateValue!=null){
    			data.put("AUTHUSER", generateValue);
    		}
    	}
    	String setCookieUrl = urlMatch(successUrl) + Const.SET_COOKIE_SERVLET_PATH;
    	data.put("setCookieUrl", setCookieUrl);
    	data.put("successUrl", successUrl);
        return ResultUtil.doSuccess(data);
    }
	
	//注销登录
	@GetMapping(value="/loginout")
	public String loginout() {//COOKIE策略注销
		authService.loginout();
		Cookie cookie = new Cookie("AUTHUSER", "");
		response.addCookie(cookie);
		return "redirect:/";
	}
    
	public void setSuccessUrl(String successUrl) {
		this.successUrl = successUrl;
	}  
	
	private Pattern pattern =  Pattern.compile("/");
	public String urlMatch(String url) {
		if(StringUtils.isBlank(url)){
			
		}
		Matcher matcher = pattern.matcher(url);
		int num = 0;
		while (matcher.find()) {
			num++;
			if(num==3){
				break;
			}
		}
		if(num==2){
			return url;
		}
		return url.substring(0,matcher.start());
	}
	

}