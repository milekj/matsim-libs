//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2011.08.04 at 02:05:46 PM CEST
//


package playground.gregor.grips.jaxb.inspire.roadtransportnetwork;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import playground.gregor.grips.jaxb.inspire.commontransportelements.TransportLinkSetType;


/**
 * <p>Java class for ERoadType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ERoadType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:x-inspire:specification:gmlas:CommonTransportElements:3.0}TransportLinkSetType">
 *       &lt;sequence>
 *         &lt;element name="europeanRouteNumber">
 *           &lt;complexType>
 *             &lt;simpleContent>
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *                 &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
 *               &lt;/extension>
 *             &lt;/simpleContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ERoadType", propOrder = {
		"europeanRouteNumber"
})
public class ERoadType
extends TransportLinkSetType
{

	@XmlElement(required = true, nillable = true)
	protected ERoadType.EuropeanRouteNumber europeanRouteNumber;

	/**
	 * Gets the value of the europeanRouteNumber property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link ERoadType.EuropeanRouteNumber }
	 * 
	 */
	public ERoadType.EuropeanRouteNumber getEuropeanRouteNumber() {
		return this.europeanRouteNumber;
	}

	/**
	 * Sets the value of the europeanRouteNumber property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link ERoadType.EuropeanRouteNumber }
	 * 
	 */
	public void setEuropeanRouteNumber(ERoadType.EuropeanRouteNumber value) {
		this.europeanRouteNumber = value;
	}

	public boolean isSetEuropeanRouteNumber() {
		return (this.europeanRouteNumber!= null);
	}


	/**
	 * <p>Java class for anonymous complex type.
	 * 
	 * <p>The following schema fragment specifies the expected content contained within this class.
	 * 
	 * <pre>
	 * &lt;complexType>
	 *   &lt;simpleContent>
	 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
	 *       &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
	 *     &lt;/extension>
	 *   &lt;/simpleContent>
	 * &lt;/complexType>
	 * </pre>
	 * 
	 * 
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	@XmlType(name = "", propOrder = {
			"value"
	})
	public static class EuropeanRouteNumber {

		@XmlValue
		protected String value;
		@XmlAttribute
		protected List<String> nilReason;

		/**
		 * Gets the value of the value property.
		 * 
		 * @return
		 *     possible object is
		 *     {@link String }
		 * 
		 */
		public String getValue() {
			return this.value;
		}

		/**
		 * Sets the value of the value property.
		 * 
		 * @param value
		 *     allowed object is
		 *     {@link String }
		 * 
		 */
		public void setValue(String value) {
			this.value = value;
		}

		public boolean isSetValue() {
			return (this.value!= null);
		}

		/**
		 * Gets the value of the nilReason property.
		 * 
		 * <p>
		 * This accessor method returns a reference to the live list,
		 * not a snapshot. Therefore any modification you make to the
		 * returned list will be present inside the JAXB object.
		 * This is why there is not a <CODE>set</CODE> method for the nilReason property.
		 * 
		 * <p>
		 * For example, to add a new item, do as follows:
		 * <pre>
		 *    getNilReason().add(newItem);
		 * </pre>
		 * 
		 * 
		 * <p>
		 * Objects of the following type(s) are allowed in the list
		 * {@link String }
		 * 
		 * 
		 */
		public List<String> getNilReason() {
			if (this.nilReason == null) {
				this.nilReason = new ArrayList<String>();
			}
			return this.nilReason;
		}

		public boolean isSetNilReason() {
			return ((this.nilReason!= null)&&(!this.nilReason.isEmpty()));
		}

		public void unsetNilReason() {
			this.nilReason = null;
		}

	}


	@Override
	public Object createNewInstance() {
		// TODO Auto-generated method stub
		return null;
	}

}