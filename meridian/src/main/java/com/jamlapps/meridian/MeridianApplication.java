package com.jamlapps.meridian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@EnableCaching
@Modulithic(systemName = "Meridian", sharedModules = "shared")
public class MeridianApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeridianApplication.class, args);
    }

}
