import scala.collection.JavaConversions._
import org.apache.commons.io.IOUtils
import java.io.FileOutputStream
import com.gargoylesoftware.htmlunit._
import com.gargoylesoftware.htmlunit.html._

object Test extends App {
  
  val wc = new WebClient()
  val p: HtmlPage = wc.getPage("https://www.ov-chipkaart.nl/login/")
  val f = p.getForms()(1)
  f.getInputByName[HtmlTextInput]("gebruikersnaam:input").setText("YOMENLAYE")
  f.getInputByName[HtmlPasswordInput]("wachtwoord:input").setText("ydlzfy83")
  
  val p2: HtmlPage = f.getButtonByName("inloggen").click()
  println(p2.asText().contains("Gebruikersnaam en/of wachtwoord incorrect"))
  
  val p3: HtmlPage = wc.getPage("https://www.ov-chipkaart.nl/mijnovchipkaart/reizenentransacties/mijnreizenentransacties/")
  val f3 = p3.getForms()(2)
  
  f3.getSelectByName("transactieTypes").getOptionByText("Reistransacties").setSelected(true)
  f3.getSelectByName("periodes").getOptionByText("Juli 2014").setSelected(true)
  
  val p4: HtmlPage = f3.getButtonByName("submitzoekopdracht").click()
  val table: HtmlTable = p4.getFirstByXPath("//table[contains(@class, 'transacties')]")
  table.getRows().foreach { row =>
    println(row.getCell(0).asText())  
  }
  
  val pdf: TextPage = p4.getFirstByXPath[HtmlAnchor]("//a[@title = 'Declaratieoverzicht maken in pdf']").click()
  IOUtils.copy(pdf.getWebResponse().getContentAsStream(), new FileOutputStream("htmlunit.pdf"))
  
  wc.closeAllWindows()
  

  println("Finished!")
  //Gebruikersnaam en/of wachtwoord incorrect

}