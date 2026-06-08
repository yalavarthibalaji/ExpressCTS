package com.iispl.dto.xml.cigf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File    : com/iispl/dto/xml/cigf/CigfBatchInfoXml.java
 * Purpose : Maps the <BatchInfo> block inside the CIGF root.
 *
 * XML produced:
 *   <BatchInfo>
 *     <BatchId>B-2026-0608-001</BatchId>
 *     <ImageCount>20</ImageCount>
 *     <TotalImageSize>1234567</TotalImageSize>
 *     <GeneratedAt>2026-06-08T14:30:00</GeneratedAt>
 *   </BatchInfo>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CigfBatchInfoXml {

    @XmlElement(name = "BatchId",        namespace = "urn:npci:cts:cigf:v1.0")
    private String batchId;

    @XmlElement(name = "ImageCount",     namespace = "urn:npci:cts:cigf:v1.0")
    private int imageCount;

    @XmlElement(name = "TotalImageSize", namespace = "urn:npci:cts:cigf:v1.0")
    private long totalImageSize;          // total bytes across all images

    @XmlElement(name = "GeneratedAt",    namespace = "urn:npci:cts:cigf:v1.0")
    private String generatedAt;

    public String getBatchId()            { return batchId; }
    public void   setBatchId(String s)    { this.batchId = s; }
    public int    getImageCount()         { return imageCount; }
    public void   setImageCount(int n)    { this.imageCount = n; }
    public long   getTotalImageSize()     { return totalImageSize; }
    public void   setTotalImageSize(long n){ this.totalImageSize = n; }
    public String getGeneratedAt()        { return generatedAt; }
    public void   setGeneratedAt(String s){ this.generatedAt = s; }
}