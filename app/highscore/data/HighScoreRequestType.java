
package highscore.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für HighScoreRequestType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="HighScoreRequestType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://big.tuwien.ac.at/we/highscore/data}UserKey"/>
 *         &lt;element ref="{http://big.tuwien.ac.at/we/highscore/data}UserData"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HighScoreRequestType", propOrder = {
    "userKey",
    "userData"
})
public class HighScoreRequestType {

    @XmlElement(name = "UserKey", namespace = "http://big.tuwien.ac.at/we/highscore/data", required = true)
    protected String userKey;
    @XmlElement(name = "UserData", namespace = "http://big.tuwien.ac.at/we/highscore/data", required = true)
    protected UserDataType userData;

    /**
     * Ruft den Wert der userKey-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUserKey() {
        return userKey;
    }

    /**
     * Legt den Wert der userKey-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUserKey(String value) {
        this.userKey = value;
    }

    /**
     * Ruft den Wert der userData-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link UserDataType }
     *     
     */
    public UserDataType getUserData() {
        return userData;
    }

    /**
     * Legt den Wert der userData-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link UserDataType }
     *     
     */
    public void setUserData(UserDataType value) {
        this.userData = value;
    }

}
