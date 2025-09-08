package com.bis.bisfrstationreadcsv;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SmsHistoryVO {
    private LocalDateTime decisionDate;
    private String operator_id;
    private String mobileNo;
    private String targetTp;
    private String damageLv;
    private String smsformId;
    private String smsValue;
}
