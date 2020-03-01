package com.mz.finalcommunity.finalcommunity;

import com.mz.finalcommunity.finalcommunity.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes =FinalcommunityApplication.class)
public class TransationTests {
    @Autowired
    private AlphaService alphaService;

    @Test
    public void testSave1(){
        Object object = alphaService.save1();
        System.out.println(object);
    }

    @Test
    public void testSave2(){
        Object object = alphaService.save2();
        System.out.println(object);
    }
}
