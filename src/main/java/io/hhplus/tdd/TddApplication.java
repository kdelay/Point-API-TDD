package io.hhplus.tdd;

import io.hhplus.tdd.database.UserPointTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TddApplication {

    public static void main(String[] args) {
        SpringApplication.run(TddApplication.class, args);
        new UserPointTable();
    }
}
