package com.vic.iot.gateway.controller;


import com.vic.iot.common.utils.PageQueryParaUtils;
import com.vic.iot.gateway.model.GrantedAuthority;
import com.vic.iot.gateway.model.User;
import com.vic.iot.gateway.model.request.UserRequest;
import com.vic.iot.gateway.model.response.UserResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

@RestController
@Slf4j
@Api(tags = "用户控制")
@RequestMapping("/users")
public class UserController extends GatewayBaseController {


    @ApiOperation(value = "注册用户")
    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(value = "register", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> register(@RequestBody UserRequest userRequest) {
        String api = gatewayServiceProperties.getUser().getPrefix() + gatewayServiceProperties.getUser().getRegister();

        User user = new User();
        BeanUtils.copyProperties(userRequest, user);

        Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new GrantedAuthority("ROLE_USER"));
        user.setAuthorities(authorities);


        return restTemplate.postForEntity(api, user, User.class);
    }

    @ApiOperation(value = "查询所有用户信息")
    @PreAuthorize("hasRole('ROLE_USER')")
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> findUsers(@PageableDefault(page = 0, size = 20) Pageable pageable) {
        String api = gatewayServiceProperties.getUser().getPrefix() + gatewayServiceProperties.getUser().getUsers();
        String url = PageQueryParaUtils.buildUrl(api, null, pageable);
        return restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, new ParameterizedTypeReference<PagedResources<UserResponse>>() {
        });
    }

    @ApiOperation(value = "查询超级用户信息(测试api, 没有这个角色, 应该返回403错误, 权限不足)")
    @PreAuthorize("hasRole('ROLE_SUPER_USER')")
    @RequestMapping(value = "/superuser", method = RequestMethod.GET)
    public Object findSuperUsers() {
        return "super user";
    }
}
