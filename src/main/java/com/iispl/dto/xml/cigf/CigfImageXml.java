package com.iispl.dto.xml.cigf;

import jakarta.xml.bind.annotation.XmlAccessType;

import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * File : com/iispl/dto/xml/cigf/CigfImageXml.java Purpose : Maps a single
 * <Image> element inside the CIGF ImageList.
 *
 * Each entry describes one cheque image (front or back) with its size and
 * SHA-256 checksum so NPCI can verify the package.
 *
 * XML produced: <Image> <SeqNo>1</SeqNo> <ChequeNo>123456</ChequeNo>
 * <Side>FRONT</Side> <FilePath>001-front.jpg</FilePath>
 * <FileSize>45678</FileSize> <Checksum>9f86d081884c7d65...</Checksum> </Image>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CigfImageXml {

	@XmlElement(name = "SeqNo", namespace = "urn:npci:cts:cigf:v1.0")
	private int seqNo;

	@XmlElement(name = "ChequeNo", namespace = "urn:npci:cts:cigf:v1.0")
	private String chequeNo;

	@XmlElement(name = "Side", namespace = "urn:npci:cts:cigf:v1.0")
	private String side; // FRONT or BACK

	@XmlElement(name = "FilePath", namespace = "urn:npci:cts:cigf:v1.0")
	private String filePath; // relative path in the export folder

	@XmlElement(name = "FileSize", namespace = "urn:npci:cts:cigf:v1.0")
	private long fileSize;

	@XmlElement(name = "Checksum", namespace = "urn:npci:cts:cigf:v1.0")
	private String checksum; // SHA-256 hex

	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int n) {
		this.seqNo = n;
	}

	public String getChequeNo() {
		return chequeNo;
	}

	public void setChequeNo(String s) {
		this.chequeNo = s;
	}

	public String getSide() {
		return side;
	}

	public void setSide(String s) {
		this.side = s;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String s) {
		this.filePath = s;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long n) {
		this.fileSize = n;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String s) {
		this.checksum = s;
	}
}