package com.iispl.dto.xml.cxf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * File    : com/iispl/dto/xml/cxf/CxfChequeListXml.java
 * Purpose : Wrapper for the list of <Cheque> elements inside CXF.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CxfChequeListXml {

    @XmlElement(name = "Cheque", namespace = "urn:npci:cts:cxf:v1.0")
    private List<CxfChequeXml> cheques = new ArrayList<>();

    public List<CxfChequeXml> getCheques()           { return cheques; }
    public void setCheques(List<CxfChequeXml> list)  { this.cheques = list; }
}