package com.bis.bisfrstationreadcsv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BisLinkServerGuardRunner implements ApplicationRunner {
    @Autowired
    private SmsHistoryService smsHistoryService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 환경 변수 또는 프로파일 체크로 분기 처리
        String env = System.getProperty("env", "local"); // 기본값 local
        String serverName = System.getProperty("serverName"); // 기본값 local
        if (!env.equals("prod")) {
            System.out.println("개발 환경에서 실행 건너뜀");
            return;
        }

        try {
            smsHistoryService.checkServer(serverName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
