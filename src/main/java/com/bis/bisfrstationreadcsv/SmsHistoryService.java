package com.bis.bisfrstationreadcsv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public interface SmsHistoryService {
    public void checkServer(String serverName) throws IOException;
}

@Service
class SmsHistoryServiceImpl implements  SmsHistoryService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void checkServer(String serverName) throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        List<SmsHistoryVO> smsHistoryVOS = this.readSmsHistory(serverName);
        if(smsHistoryVOS != null && smsHistoryVOS.size() > 0) {
            for (SmsHistoryVO smsHistoryVO : smsHistoryVOS) {
                if(smsHistoryVO.getSmsValue().contains("복구완료")){
                    // 문제 없음. 로그 남기기용
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
                    System.out.println("예외문자 : " + smsHistoryVO.getSmsValue());
                }
            }
        }

    }

    private void killProcess(String processName) {
        try {
            String command = String.format("taskkill /F /IM %s", processName);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();  // 종료될 때까지 대기
            System.out.println(processName + " 종료 완료");
        } catch (Exception e) {
            System.err.println(processName + " 종료 실패: " + e.getMessage());
        }
    }

    private List<SmsHistoryVO> readSmsHistory(String servername) throws IOException {

        String query = String.format("select *\n" +
                "from SMS_HISTORY\n" +
                "where mobile_no = '010-4693-8128'\n" +
                "and sms_value like '%<복구%'\n" +
                "and sms_value like '%s'\n" +
                "and decision_date between systimestamp - interval '20' second and systimestamp\n" +
                "order by decision_date desc, smsForm_id asc" +
                "",servername);

        List<SmsHistoryVO> smsHistoryVOS = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(SmsHistoryVO.class));

        return smsHistoryVOS;
    }

}
