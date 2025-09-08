package com.bis.bisfrstationreadcsv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SmsScheduler {

    @Autowired
    private SmsHistoryServiceImpl smsHistoryService;

    // 1분마다 실행 (fixedRate = 60000ms)
    @Scheduled(fixedDelay = 3000)
    public void checkSmsEveryMinute() throws IOException {
        // 환경 변수 또는 프로파일 체크로 분기 처리
        String env = System.getProperty("env", "local"); // 기본값 local
        String serverName = System.getProperty("serverName"); // 기본값 local
        if (!env.equals("prod")) {
            System.out.println("[" + serverName + "] 개발/테스트 환경에서 실행 건너뜀 (env=" + env + ")");
            return;
        }

        try {
            smsHistoryService.checkServer(serverName);
        } catch (IOException e) {
            System.err.println("[" + serverName + "] SMS 체크 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
