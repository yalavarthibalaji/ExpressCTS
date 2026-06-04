package com.iispl.jaxb;

import java.util.List;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "BPXF")
@XmlAccessorType(XmlAccessType.FIELD)
public class BpxfRoot {

    @XmlElement(name = "Header")
    private BpxfHeader header;

    @XmlElementWrapper(name = "Cheques")
    @XmlElement(name = "Cheque")
    private List<BpxfCheque> cheques;

    public BpxfHeader        getHeader()          { return header; }
    public void              setHeader(BpxfHeader v){ this.header = v; }

    public List<BpxfCheque> getCheques()         { return cheques; }
    public void               setCheques(List<BpxfCheque> v){ this.cheques = v; }
}