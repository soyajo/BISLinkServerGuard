package com.bis.bisfrstationreadcsv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public interface SmsHistoryService {
    public void checkServer(String serverName, String telNum, String selectDelay) throws IOException;
}

@Service
class SmsHistoryServiceImpl implements  SmsHistoryService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String LOG_DIR = "C:\\work\\BISLinkServerGuard\\log\\";

    private String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    public void checkServer(String serverName, String telNum, String selectDelay) throws IOException {

        List<SmsHistoryVO> smsHistoryVOS = this.readSmsHistory(serverName, telNum, selectDelay);
        if(smsHistoryVOS != null && smsHistoryVOS.size() > 0) {
            for (SmsHistoryVO smsHistoryVO : smsHistoryVOS) {
                if(smsHistoryVO.getSmsValue().contains("복구완료")){
                    break;
                }else if(smsHistoryVO.getSmsValue().contains("복구필요")) {
                    // 재기동 시작
                    if (serverName.contains("EB")) {
                        killProcess("EBDataClt1.exe");
                        killProcess("EBCommclt1.exe");

                    } else if (serverName.contains("지자체")) {
                        killProcess("PTIECommsvr.exe");
                        killProcess("PTIEDataSvr.exe");
                    } else if (serverName.contains("데이터체크")) {
                        // 데이터 체크 프로세스 작성
                    }else{
                        continue;
                    }
                    // 자동으로 켜지는 건 알아서 다른 프로그램에서 됌

                }else {
                    // 예외문자 확인
                    System.out.println(LocalDateTime.now() + " - " + serverName + " - 예외문자 : " + smsHistoryVO.getSmsValue());
                }
            }
        }

    }

    private void killProcess(String processName) {
        writeLog(today, processName + " : 종료 시작");
        System.out.println(LocalDateTime.now() + " - " + processName + " - 종료 시작");
        try {
            String command = String.format("taskkill /F /IM %s", processName);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();  // 종료될 때까지 대기
            writeLog(today, processName + " 종료 완료");
            System.out.println(LocalDateTime.now() + " - " + processName + " - 종료 완료");
        } catch (Exception e) {
            writeLog(today, processName + " 종료 실패: " + e.getMessage());
            System.out.println(LocalDateTime.now() + " - " + processName + " - 종료 실패");
        }
    }

    private List<SmsHistoryVO> readSmsHistory(String servername, String telNum, String selectDelay) throws IOException {
        System.out.println(LocalDateTime.now() + " - " + servername + " - 조회 시작");
        String query = String.format("select *\n" +
                "from SMS_HISTORY\n" +
                "where mobile_no = '%s'\n" +
                "and sms_value like '%%<복구%%'\n" +
                "and sms_value like '%%%s%%'\n" +
                "and decision_date between systimestamp - interval '%s' second and systimestamp\n" +
                "order by decision_date desc, smsForm_id asc" +
                "", telNum, servername, selectDelay);

        List<SmsHistoryVO> smsHistoryVOS = new ArrayList<>();
        smsHistoryVOS = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(SmsHistoryVO.class));

        System.out.println(LocalDateTime.now() + " - " + servername + " - 조회 끝(리스트수 : " + smsHistoryVOS.size() + " 건)");
        return smsHistoryVOS;
    }

    private void writeLog(String date, String message) {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdirs(); // 폴더 없으면 생성
        }

        File logFile = new File(dir, date + ".log");
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
            bw.write(java.time.LocalDateTime.now() + " - " + message);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
