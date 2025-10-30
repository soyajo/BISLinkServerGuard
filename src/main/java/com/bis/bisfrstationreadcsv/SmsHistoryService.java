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
import java.util.Arrays;
import java.util.Collections;
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
        // ✅ 1. 먼저 중복 프로세스 감지 및 종료
        List<String> targetProcesses = getTargetProcesses(serverName);

        for (String processName : targetProcesses) {
            List<Integer> pids = getProcessPids(processName);

            if (pids.size() > 1) { // 동일 프로세스 2개 이상일 경우
                System.out.println("⚠ 중복 프로세스 발견: " + processName + " (" + pids.size() + "개)");
                // 첫 번째만 남기고 나머지는 종료
                for (int i = 1; i < pids.size(); i++) {
                    killProcessByPid(pids.get(i));
                }
            } else if (pids.size() == 1) {
                System.out.println("✅ 정상: " + processName + " (1개 실행 중)");
            } else {
                System.out.println("❌ 실행 중 아님: " + processName);
            }
        }

        // ✅ 2. 이후 SMS 내역 확인 및 복구 처리
        List<SmsHistoryVO> smsHistoryVOS = this.readSmsHistory(serverName, telNum, selectDelay);
        if(smsHistoryVOS != null && smsHistoryVOS.size() > 0) {
            for (SmsHistoryVO smsHistoryVO : smsHistoryVOS) {
                if(smsHistoryVO.getSmsValue().contains("복구완료")){
                    break;
                }else if(smsHistoryVO.getSmsValue().contains("복구필요")) {
                    // 재기동 시작
                    if (serverName.contains("EB")) {
                        killProcess("EBDataClt1.exe");
                        killProcess("EBCommClt1.exe");
                        killProcess("EBDataClt.exe");
                        killProcess("EBCommClt.exe");
                    } else if (serverName.contains("지자체")) {
                        killProcess("PTIECommsvr.exe");
                        killProcess("PTIEDataSvr.exe");
                    } else if (serverName.contains("시설물")) {
                        // 데이터 체크 프로세스 작성
                        killProcess("SDCSvr.exe");
                        killProcess("SDDSvr.exe");
                        killProcess("DataCheckprj.exe");
                    }
                    break;
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

    // ✅ 서버 이름에 따른 감시 대상 프로세스 목록
    private static List<String> getTargetProcesses(String serverName) {
        if (serverName.contains("EB")) {
            return Arrays.asList("EBDataClt1", "EBCommClt1", "EBDataClt", "EBCommClt");
        } else if (serverName.contains("지자체")) {
            return Arrays.asList("PTIECommsvr",  "PTIEDatasvr", "PTIECommSvr", "PTIEDataSvr");
        } else if (serverName.contains("시설물")) {
            return Arrays.asList("SDCSvr", "SDDSvr", "DataCheckprj");
        }
        return Collections.emptyList();
    }

    // ✅ 해당 프로세스 이름으로 실행 중인 PID 목록 조회
    private static List<Integer> getProcessPids(String processName) {
        List<Integer> pidList = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(
                    new String[]{"cmd", "/c", "tasklist /FI \"IMAGENAME eq " + processName + ".exe\""}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(processName)) {
                    // 예시: EBDataClt.exe          1234 Console    1     14,232 K
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 1 && parts[1].matches("\\d+")) {
                        pidList.add(Integer.parseInt(parts[1]));
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pidList;
    }

    // ✅ PID로 프로세스 종료
    private static void killProcessByPid(int pid) {
        try {
            Process killProcess = Runtime.getRuntime().exec(
                    new String[]{"cmd", "/c", "taskkill /PID " + pid + " /F"}
            );
            killProcess.waitFor();
            System.out.println("🛑 종료됨: PID " + pid);
        } catch (Exception e) {
            System.err.println("❌ 종료 실패 (PID: " + pid + "): " + e.getMessage());
        }
    }

}
