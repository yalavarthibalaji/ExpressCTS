package com.iispl.dto.xml;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File    : com/iispl/dto/xml/ChequeListXml.java
 * Purpose : Maps the <cheques> wrapper element.
 *           Contains a list of individual <cheque> elements.
 *
 * XML maps to:
 *   <cheques>
 *       <cheque> ... </cheque>
 *       <cheque> ... </cheque>
 *   </cheques>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ChequeListXml {

    @XmlElement(name = "cheque")
    private List<ChequeXml> chequeList;

    // ── Getters and Setters ──

    public List<ChequeXml> getChequeList() {
        return chequeList;
    }

    public void setChequeList(List<ChequeXml> chequeList) {
        this.chequeList = chequeList;
    }
}