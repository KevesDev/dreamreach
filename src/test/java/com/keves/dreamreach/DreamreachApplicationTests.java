package com.keves.dreamreach;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // This layers application-test.properties over the main config
class DreamreachApplicationTests {

    @Test
    void contextLoads() {
    }

}