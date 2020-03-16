package com.mz.community;

import com.mz.community.util.MailClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@SpringBootTest
@ContextConfiguration(classes =CommunityApplication.class)
public class MailTest {
    @Autowired
    private MailClient mailClient;
    @Test
    public void testMail(){
        mailClient.sendMail("2686224016@qq.com","test","test");
    }
}