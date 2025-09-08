package com.bis.bisfrstationreadcsv;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling  // 스케줄링 활성화
public class BisFrStationReadCsvApplication {

    public static void main(String[] args) {
        SpringApplication.run(BisFrStationReadCsvApplication.class, args);
    }

}
