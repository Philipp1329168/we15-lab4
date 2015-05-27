
package highscore.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für UserDataType complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="UserDataType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Loser" type="{http://big.tuwien.ac.at/we/highscore/data}UserType"/>
 *         &lt;element name="Winner" type="{http://big.tuwien.ac.at/we/highscore/data}UserType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UserDataType", propOrder = {
    "loser",
    "winner"
})
public class UserDataType {

    @XmlElement(name = "Loser", required = true)
    protected UserType loser;
    @XmlElement(name = "Winner", required = true)
    protected UserType winner;

    /**
     * Ruft den Wert der loser-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link UserType }
     *     
     */
    public UserType getLoser() {
        return loser;
    }

    /**
     * Legt den Wert der loser-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link UserType }
     *     
     */
    public void setLoser(UserType value) {
        this.loser = value;
    }

    /**
     * Ruft den Wert der winner-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link UserType }
     *     
     */
    public UserType getWinner() {
        return winner;
    }

    /**
     * Legt den Wert der winner-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link UserType }
     *     
     */
    public void setWinner(UserType value) {
        this.winner = value;
    }

}
