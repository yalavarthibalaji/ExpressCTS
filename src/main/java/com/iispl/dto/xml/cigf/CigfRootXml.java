package com.iispl.dto.xml.cigf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * File    : com/iispl/dto/xml/cigf/CigfRootXml.java
 * Purpose : Root JAXB DTO for the outgoing CIGF (Cheque Image Group File).
 *           CIGF is the image manifest companion to CXF — lists every image
 *           file by reference, size and checksum so NPCI can verify the
 *           image package on receive.
 *
 * XML structure produced:
 *
 *   <CIGF xmlns="urn:npci:cts:cigf:v1.0" version="1.0">
 *     <BatchInfo> ... </BatchInfo>
 *     <ImageList>
 *       <Image> ... </Image>
 *       ...
 *     </ImageList>
 *   </CIGF>
 */
@XmlRootElement(name = "CIGF", namespace = "urn:npci:cts:cigf:v1.0")
@XmlAccessorType(XmlAccessType.FIELD)
public class CigfRootXml {

    @XmlAttribute(name = "version")
    private String version = "1.0";

    @XmlElement(name = "BatchInfo", namespace = "urn:npci:cts:cigf:v1.0")
    private CigfBatchInfoXml batchInfo;

    @XmlElement(name = "ImageList", namespace = "urn:npci:cts:cigf:v1.0")
    private CigfImageListXml imageList;

    public String getVersion() { return version; }
    public void   setVersion(String v) { this.version = v; }

    public CigfBatchInfoXml getBatchInfo() { return batchInfo; }
    public void             setBatchInfo(CigfBatchInfoXml b) { this.batchInfo = b; }

    public CigfImageListXml getImageList() { return imageList; }
    public void             setImageList(CigfImageListXml l) { this.imageList = l; }
}