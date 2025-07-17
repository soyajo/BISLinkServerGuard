package com.bis.bisfrstationreadcsv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FrStationVo {
    private String stationId;
    private String stationEngNm;
    private String stationChnNm;
    private String stationJpnNm;
    private String stationBtnNm;
}
