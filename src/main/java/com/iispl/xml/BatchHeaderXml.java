package com.iispl.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

// maps to <BatchHeader> inside XML
@XmlRootElement(name = "BatchHeader")
public class BatchHeaderXml {

    private String branchCode;
    private String clearingDate;
    private String clearingSessionRef;
    private String route;
    private int totalCheques;
    private long totalAmount;

    @XmlElement(name = "BranchCode")
    public String getBranchCode() {
        return branchCode;
    }

    public void setBranchCode(String branchCode) {
        this.branchCode = branchCode;
    }

    @XmlElement(name = "ClearingDate")
    public String getClearingDate() {
        return clearingDate;
    }

    public void setClearingDate(String clearingDate) {
        this.clearingDate = clearingDate;
    }

    @XmlElement(name = "ClearingSessionRef")
    public String getClearingSessionRef() {
        return clearingSessionRef;
    }

    public void setClearingSessionRef(String clearingSessionRef) {
        this.clearingSessionRef = clearingSessionRef;
    }

    @XmlElement(name = "Route")
    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    @XmlElement(name = "TotalCheques")
    public int getTotalCheques() {
        return totalCheques;
    }

    public void setTotalCheques(int totalCheques) {
        this.totalCheques = totalCheques;
    }

    @XmlElement(name = "TotalAmount")
    public long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
    }
}