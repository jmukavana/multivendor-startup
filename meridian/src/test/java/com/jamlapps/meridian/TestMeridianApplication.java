package com.jamlapps.meridian;

import org.springframework.boot.SpringApplication;

public class TestMeridianApplication {

    public static void main(String[] args) {
        SpringApplication.from(MeridianApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
