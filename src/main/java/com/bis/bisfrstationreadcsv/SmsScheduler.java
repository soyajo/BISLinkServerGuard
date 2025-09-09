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
    @Scheduled(fixedDelayString = "#{systemProperties['batchDelay'] ?: '30000'}") // -DbatchDelay 설정 가능, 없으면 30초, 배치를 몇초동안 동작시키는건지
    public void checkSmsEveryMinute() throws IOException {
        // 환경 변수 또는 프로파일 체크로 분기 처리
        String env = System.getProperty("env", "local"); // 기본값 local
        String serverName = System.getProperty("serverName");
        String telNum = System.getProperty("telNum");
        String selectDelay = System.getProperty("selectDelay", "30"); // -DselectDelay 설정 가능, 없으면 30초, 조회 몇초 전까지 조회할것인지

        if(serverName == null || serverName.trim().equals("")){
            System.out.println("서버명을 지정해주세요.(-DserverName=[])");
            return;
        }
        if(telNum == null || telNum.trim().equals("")){
            System.out.println("연락처를 지정해주세요.(-DtelNum=[])");
            return;
        }

        if (!env.equals("prod")) {
            System.out.println("[" + serverName + "] 개발/테스트 환경에서 실행 건너뜀 (env=" + env + ")");
            return;
        }

        try {
            smsHistoryService.checkServer(serverName, telNum, selectDelay);
        } catch (IOException e) {
            System.err.println("[" + serverName + "] SMS 체크 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
