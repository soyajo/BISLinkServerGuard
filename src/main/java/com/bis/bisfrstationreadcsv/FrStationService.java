package com.bis.bisfrstationreadcsv;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public interface FrStationService {
    public void execute() throws IOException;
}

@Service
class FrStationServiceImpl implements  FrStationService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

//    public FrStationServiceImpl() {
//        // Oracle DataSource 설정
//        DriverManagerDataSource dataSource = new DriverManagerDataSource();
//        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
//        dataSource.setUrl("jdbc:oracle:thin:@192.168.103.70:1521/frgbis
//        dataSource.setUsername("frgbis");
//        dataSource.setPassword("frgbis");
//
//        this.jdbcTemplate = new JdbcTemplate(dataSource);
//    }

    public void execute() throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filePath = "D:/GGBMS/FTPRoot/BMS_FTP/LocalUser/bmsftp/" + today + "/foreign_station.txt";

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("파일이 존재하지 않습니다: " + filePath);
            System.exit(1);  // 애플리케이션 강제 종료
        }

        List<FrStationVo> stations = readFile(filePath);

        truncateTable();
        insertStations(stations);
    }

    private List<FrStationVo> readFile(String filePath) throws IOException {
        List<FrStationVo> stationList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    continue;
                }

                String[] parts = line.split("\\|");
                if (parts.length < 5) continue;

                FrStationVo station = new FrStationVo();
                station.setStationId(parts[0].trim());
                station.setStationEngNm(parts[1].trim());
                station.setStationChnNm(parts[2].trim());
                station.setStationJpnNm(parts[3].trim());
                station.setStationBtnNm(parts[4].trim());

                stationList.add(station);
            }
        }

        return stationList;
    }

    private void truncateTable() {
        jdbcTemplate.execute("TRUNCATE TABLE fr_station");
    }

    private void insertStations(List<FrStationVo> stations) {
        String sql = "INSERT INTO fr_station (station_id, station_eng_nm, station_chn_nm, station_jpn_nm, station_btn_nm) " +
                "VALUES (?, ?, ?, ?, ?)";

        for (FrStationVo station : stations) {
            jdbcTemplate.update(sql,
                    station.getStationId(),
                    station.getStationEngNm(),
                    station.getStationChnNm(),
                    station.getStationJpnNm(),
                    station.getStationBtnNm());
        }
    }
}
