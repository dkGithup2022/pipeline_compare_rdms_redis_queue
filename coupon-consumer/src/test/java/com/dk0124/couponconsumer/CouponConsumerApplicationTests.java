package com.dk0124.couponconsumer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = "spring.config.name=application-core")
class CouponConsumerApplicationTests {

    @Test
    void contextLoads() {
    }

}
