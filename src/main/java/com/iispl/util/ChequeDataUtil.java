package com.iispl.util;

import com.iispl.entity.OutwardChequeDummy;

import java.util.ArrayList;
import java.util.List;

/**
 * ChequeDataUtil.java
 * Hardcoded cheque data matching the prototype's sample data exactly.
 *
 * Package  : com.iispl.util
 * Pattern  : Utility (pure static helper)
 *
 * To switch to DB: Replace buildDummyCheques() with a DAO call.
 * All 20 sample cheques from the prototype are included here.
 */
public class ChequeDataUtil {

    /**
     * Returns 20 dummy cheques that match the prototype's CHQ001-CHQ020.
     * These are pre-loaded into the Maker Queue on the dashboard.
     */
    public static List<OutwardChequeDummy> buildDummyCheques(String batchId) {
        List<OutwardChequeDummy> list = new ArrayList<>();

        list.add(make("CHQ001","TXN20250415001","000123456","State Bank of India","MG Road Branch","SBIN0001234","600002010","15-Apr-2025","15-Apr-2025","Rajesh Kumar","50100123456789","ABC Traders","Two Lakh Fifty Thousand Rupees Only",250000,"ACTIVE","","20100987654321","PASS",batchId));
        list.add(make("CHQ002","TXN20250415002","000234567","HDFC Bank","Calicut Branch","HDFC0002345","673240002","12-Apr-2025","15-Apr-2025","Priya Nair","50100234567890","Kerala Distributors","Seventy Five Thousand Rupees Only",75000,"ACTIVE","","20100876543210","PASS",batchId));
        list.add(make("CHQ003","TXN20250415003","000345678","ICICI Bank","Kozhikode Branch","ICIC0003456","673229003","10-Apr-2025","15-Apr-2025","Mohammed Ashraf","628601234567","Sunrise Enterprises","Five Lakh Rupees Only",500000,"ACTIVE","","20100765432109","FAIL",batchId));
        list.add(make("CHQ004","TXN20250415004","000456789","Canara Bank","Beach Road Branch","CNRB0004567","673015004","14-Apr-2025","15-Apr-2025","Sunitha Menon","0423101012345","National Suppliers","Twelve Lakh Rupees Only",1200000,"ACTIVE","","20100654321098","PASS",batchId));
        list.add(make("CHQ005","TXN20250415005","000567890","Union Bank of India","Palayam Branch","UBIN0005678","695026005","13-Apr-2025","15-Apr-2025","Deepa Pillai","05100123456001","Star Agencies","Sixty Two Thousand Five Hundred Rupees Only",62500,"ACTIVE","","20100543210987","PASS",batchId));
        list.add(make("CHQ006","TXN20250415006","000678901","Punjab National Bank","SM Street Branch","PUNB0067890","673024006","11-Apr-2025","15-Apr-2025","Arun Krishnan","01990123456789","Malabar Textiles","One Lakh Twenty Thousand Rupees Only",120000,"ACTIVE","","20100432109876","PASS",batchId));
        list.add(make("CHQ007","TXN20250415007","000789012","Bank of Baroda","Thrissur Branch","BARB0THRSSR","680012007","09-Apr-2025","15-Apr-2025","Meera Suresh","30050123456","Golden Hardware","Thirty Five Thousand Rupees Only",35000,"ACTIVE","","20100321098765","PASS",batchId));
        list.add(make("CHQ008","TXN20250415008","000890123","Axis Bank","Palakkad Branch","UTIB0008901","678001008","15-Apr-2025","15-Apr-2025","Suresh Babu Nair","91700345678901","Vishnu Pharma","Eight Lakh Seventy Five Thousand Rupees Only",875000,"ACTIVE","","20100210987654","PASS",batchId));
        list.add(make("CHQ009","TXN20250415009","000901234","Indian Overseas Bank","Trivandrum HQ","IOBA0009012","695003009","14-Apr-2025","15-Apr-2025","Lakshmi Varma","0419200345678","Green Agro Farms","Forty Five Thousand Rupees Only",45000,"ACTIVE","","20100109876543","PASS",batchId));
        list.add(make("CHQ010","TXN20250415010","001012345","Federal Bank","Ernakulam Branch","FDRL0001012","682049010","08-Jan-2025","15-Apr-2025","Thomas Varghese","14800200345678","Metro Constructions","Two Lakh Rupees Only",200000,"ACTIVE","","20100098765432","PASS",batchId));
        list.add(make("CHQ011","TXN20250415011","001123456","South Indian Bank","Kottayam Branch","SIBL0001123","686018011","13-Apr-2025","15-Apr-2025","Anitha George","0701053000234","Asha Garments","Ninety Thousand Rupees Only",90000,"ACTIVE","","20100987123456","PASS",batchId));
        list.add(make("CHQ012","TXN20250415012","001234567","State Bank of India","Palayam Branch","SBIN0001234","695002012","12-Apr-2025","15-Apr-2025","Ravi Shankar","50100345678901","Bright Electricals","Three Lakh Rupees Only",300000,"BOUNCED","Insufficient Funds (previous return)","20100876123456","PASS",batchId));
        list.add(make("CHQ013","TXN20250415013","001345678","HDFC Bank","Kozhikode City Branch","HDFC0001345","673240013","10-Apr-2025","15-Apr-2025","Fatima Beevi","50100456789012","Royal Motors","Seven Lakh Fifty Thousand Rupees Only",750000,"ACTIVE","","20100765123456","PASS",batchId));
        list.add(make("CHQ014","TXN20250415014","001456789","ICICI Bank","Thrissur Branch","ICIC0001456","680229014","15-Apr-2025","15-Apr-2025","Vinod Kumar","628612345678","JK Timber Works","Fifteen Thousand Rupees Only",15000,"ACTIVE","","20100654123456","PASS",batchId));
        list.add(make("CHQ015","TXN20250415015","001567890","Canara Bank","Alappuzha Branch","CNRB0001567","688015015","11-Apr-2025","15-Apr-2025","Sreelatha Mohan","0423202012345","Pearl Jewellers","Eighteen Lakh Rupees Only",1800000,"ACTIVE","","20100543123456","PASS",batchId));
        list.add(make("CHQ016","TXN20250415016","001678901","Axis Bank","Kochi Branch","UTIB0001678","682001016","14-Apr-2025","15-Apr-2025","Pramod Nambiar","91700123456789","BlueStar Services","Four Lakh Twenty Thousand Rupees Only",420000,"ACTIVE","","20100432123456","PASS",batchId));
        list.add(make("CHQ017","TXN20250415017","001789012","Federal Bank","Thrissur Branch","FDRL0001789","680049017","13-Apr-2025","15-Apr-2025","Asha Krishnan","14800100123456","Sunrise Medical","One Lakh Five Thousand Rupees Only",105000,"ACTIVE","","20100321123456","PASS",batchId));
        list.add(make("CHQ018","TXN20250415018","001890123","Punjab National Bank","Kannur Branch","PUNB0001890","670024018","09-Apr-2025","15-Apr-2025","Bijoy Antony","01990234567890","Coconut Oils Ltd","Fifty Five Thousand Rupees Only",55000,"ACTIVE","","20100210123456","PASS",batchId));
        list.add(make("CHQ019","TXN20250415019","001901234","Union Bank of India","Kollam Branch","UBIN0001901","691026019","12-Apr-2025","15-Apr-2025","Nisha Babu","05100345678901","TechZone Computers","Two Lakh Eighty Thousand Rupees Only",280000,"ACTIVE","","20100109123456","PASS",batchId));
        list.add(make("CHQ020","TXN20250415020","002012345","South Indian Bank","Palakkad Branch","SIBL0002012","678018020","15-Apr-2025","15-Apr-2025","George Mathew","0701053000456","Greenfield Resorts","Ten Lakh Rupees Only",1000000,"ACTIVE","","20100098123456","PASS",batchId));

        return list;
    }

    // ── Builder helper ────────────────────────────────────────────────
    private static OutwardChequeDummy make(
            String id, String txnId, String chequeNo,
            String bankName, String branchName,
            String ifsc, String micr,
            String chequeDate, String presentDate,
            String drawerName, String drawerAcc,
            String payeeName, String amountWords, long amountFig,
            String chequeStatus, String bounceReason,
            String depositorAcc, String iqaStatus, String batchId) {

        OutwardChequeDummy c = new OutwardChequeDummy();
        c.setId(id);
        c.setTransactionId(txnId);
        c.setChequeNumber(chequeNo);
        c.setBankName(bankName);
        c.setBranchName(branchName);
        c.setIfscCode(ifsc);
        c.setMicrCode(micr);
        c.setChequeDate(chequeDate);
        c.setPresentationDate(presentDate);
        c.setDrawerName(drawerName);
        c.setDrawerAccountNumber(drawerAcc);
        c.setPayeeName(payeeName);
        c.setAmountInWords(amountWords);
        c.setAmountInFigures(amountFig);
        c.setChequeStatus(chequeStatus);
        c.setBounceReason(bounceReason);
        c.setDepositorAcc(depositorAcc);
        c.setIqaStatus(iqaStatus);
        c.setBatchId(batchId);
        c.setMakerStatus("pending");
        c.setCheckerStatus("pending");
        c.setReviewed(false);
        c.setIqaManualEntry("FAIL".equals(iqaStatus));
        return c;
    }

    private ChequeDataUtil() {}
}