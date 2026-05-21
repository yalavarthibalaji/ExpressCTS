package com.iispl.xml;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

// maps to <Cheques> which holds a list of <Cheque>
@XmlRootElement(name = "Cheques")
public class ChequeListXml {

    private List<ChequeXml> chequeList;

    @XmlElement(name = "Cheque")
    public List<ChequeXml> getChequeList() {
        return chequeList;
    }

    public void setChequeList(List<ChequeXml> chequeList) {
        this.chequeList = chequeList;
    }
}