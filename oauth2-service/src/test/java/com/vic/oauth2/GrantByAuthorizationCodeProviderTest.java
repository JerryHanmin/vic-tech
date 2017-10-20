package com.vic.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vic.oauth2.server.AuthApp;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/***
 * 通过授权码方式访问受限资源
 *
 * @author hanmin
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AuthApp.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class GrantByAuthorizationCodeProviderTest extends OAuth2Test {

    @Value("${local.server.port}")
    private int port;

    @Test
    public void getJwtTokenByAuthorizationCode()
            throws IOException, URISyntaxException, InvalidJwtException {
        //用户名,密码
        String userName = "admin";
        String password = "admin";

        //步骤一：模拟返回受保护资源
        String redirectUrl = "http://localhost:" + port + "/security/resources/user";
        //通过用户名密码发起请求
        ResponseEntity<String> response = new TestRestTemplate(userName, password).postForEntity(
                "http://localhost:" + port + "/security/oauth/authorize?response_type=code&client_id=normal-app&redirect_uri={redirectUrl}",
                null, String.class, redirectUrl);
        //判断返回状态是否是200,如果不是跑出异常
        assertEquals(HttpStatus.OK, response.getStatusCode());
        //获取cookie里面的JSSIONID cookie
        List<String> setCookie = response.getHeaders().get("Set-Cookie");
        String jSessionIdCookie = setCookie.get(0);
        String cookieValue = jSessionIdCookie.split(";")[0];

        //组织一个http请求头部,放入cookie
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", cookieValue);
        //通过用户名密码再次发起请求获取授权码
        response = new TestRestTemplate(userName, password).postForEntity(
                "http://localhost:" + port + "/security/oauth/authorize?response_type=code&client_id=normal-app&redirect_uri={redirectUrl}&user_oauth_approval=true&authorize=Authorize",
                new HttpEntity<>(headers), String.class, redirectUrl);
        //判断是否是302跳转状态
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertNull(response.getBody());

        String location = response.getHeaders().get("Location").get(0);
        //获取url
        URI locationURI = new URI(location);
        //获取url后面的请求参数，即获取授权码code=xxx
        String query = locationURI.getQuery();

        //组织授权码获取access token
        location = "http://localhost:" + port + "/security/oauth/token?" + query
                + "&grant_type=authorization_code&client_id=normal-app&redirect_uri={redirectUrl}";

        response = new TestRestTemplate("normal-app", "").postForEntity(location, new HttpEntity<>(new HttpHeaders()),
                String.class, redirectUrl);
        //判断获取access token 是否成功
        assertEquals(HttpStatus.OK, response.getStatusCode());

        //获取access_token信息
        HashMap<?, ?> jwtMap = new ObjectMapper().readValue(response.getBody(), HashMap.class);
        String accessToken = (String) jwtMap.get("access_token");

        JwtContext jwtContext = jwtConsumer.process(accessToken);

        //打印出返回的授权信息
        logJWTClaims(jwtContext);

        assertEquals(userName, jwtContext.getJwtClaims().getClaimValue("user_name"));

        //组织授权后的头部
        headers = new HttpHeaders();
        //oauth2的授权访问头部类型为Bearer
        headers.set("Authorization", "Bearer " + accessToken);

        //测试访问几个授权保护的url,分别是没有权限的/client,有权限的/user,有权限的/principal,有权限的/roles

        response = new TestRestTemplate().exchange("http://localhost:" + port + "/security/resources/client", HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());

        response = new TestRestTemplate().exchange("http://localhost:" + port + "/security/resources/user", HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        response = new TestRestTemplate().exchange("http://localhost:" + port + "/security/resources/principal", HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        assertEquals(userName, response.getBody());

        response = new TestRestTemplate().exchange("http://localhost:" + port + "/security/resources/roles", HttpMethod.GET,
                new HttpEntity<>(null, headers), String.class);
        assertEquals("[{\"authority\":\"ROLE_USER\"}]", response.getBody());
    }

}