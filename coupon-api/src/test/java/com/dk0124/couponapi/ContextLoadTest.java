package com.dk0124.couponapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest

@ActiveProfiles("local")
@TestPropertySource(properties = "spring.config.name=application-core")
public class ContextLoadTest{
    @Test
    void empty(){

    }
}
