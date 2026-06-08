package com.iispl.dto.xml.cigf;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * File : com/iispl/dto/xml/cigf/CigfImageListXml.java Purpose : Wrapper for the
 * list of <Image> elements inside CIGF.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class CigfImageListXml {

	@XmlElement(name = "Image", namespace = "urn:npci:cts:cigf:v1.0")
	private List<CigfImageXml> images = new ArrayList<>();

	public List<CigfImageXml> getImages() {
		return images;
	}

	public void setImages(List<CigfImageXml> l) {
		this.images = l;
	}
}