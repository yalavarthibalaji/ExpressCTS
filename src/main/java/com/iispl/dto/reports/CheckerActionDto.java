package com.iispl.dto.reports;

/**
 * File    : com/iispl/dto/reports/CheckerActionDto.java
 * Purpose : One row in the Checker Action Report.
 *           Counts Pass / Reject / Refer actions per checker from
 *           the outward_checker_actions audit table.
 */
public class CheckerActionDto {

    private String checkerName;
    private int    passedCount;
    private int    rejectedCount;
    private int    referredCount;
    private int    totalActions;
    private int    batchesHandled;

    public CheckerActionDto() {}

    public String getCheckerName()    { return checkerName; }
    public void   setCheckerName(String s) { this.checkerName = s; }
    public int    getPassedCount()    { return passedCount; }
    public void   setPassedCount(int n) { this.passedCount = n; }
    public int    getRejectedCount()  { return rejectedCount; }
    public void   setRejectedCount(int n) { this.rejectedCount = n; }
    public int    getReferredCount()  { return referredCount; }
    public void   setReferredCount(int n) { this.referredCount = n; }
    public int    getTotalActions()   { return totalActions; }
    public void   setTotalActions(int n) { this.totalActions = n; }
    public int    getBatchesHandled() { return batchesHandled; }
    public void   setBatchesHandled(int n) { this.batchesHandled = n; }
}