package controllers

import java.net._
import javax.inject._

import org.jsoup._
import org.jsoup.nodes._
import org.jsoup.select._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import scala.collection.JavaConverters._

case class WebsiteInfo (url: String, title: String, version: String, headings: scala.collection.mutable.LinkedHashMap[String, Int], links: scala.collection.mutable.LinkedHashMap[String, Int], isLoginPage: Boolean)
/**
 * This controller creates an `Action` to handle HTTP Website information requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() extends Controller {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  @Inject()
  def index = Action { implicit request =>
    Ok(views.html.index(HomeController.httpUrlForm))
  }

  /**
    * Action to fetch the Website information
    * @return HTML document information as Json Array
    */
  def fetch() = Action { implicit request =>

    val values = HomeController.httpUrlForm.bindFromRequest.data
    val website = values("website")
    val document = Jsoup.connect(website).get
    val docType = document.childNode(0).outerHtml
    var version: Option[String] = None

    if (docType.matches("(?i)<!doctype\\s*html>")) {
      version = Some("HTML5")
    }
    else if (docType.matches("(<\\?.*?\\?>)?\\s*<!DOCTYPE\\s+([a-zA-Z_][a-zA-Z0-9]*\\s+[a-zA-Z_][a-zA-Z0-9]*\\s+\"[^\"]*\")[^>]*>")) {
      val pattern = "HTML\\s+[0-9]+.?[0-9]+[0-9]*".r
      version = Some(pattern.findFirstIn(docType).getOrElse("UNKNOWN"))
    }

    val title = document.title

    val headings = populateHeadingsMap(document)

    val linksMap = detectPageLinks(website, document)

    val loginPage = isLoginPage(document)


    implicit val websiteInfoWrites = new Writes[WebsiteInfo] {
      def writes(websiteInfo: WebsiteInfo) = Json.obj(
        "url" -> websiteInfo.url,
        "title" -> websiteInfo.title,
        "version" -> websiteInfo.version,
        "headings" -> websiteInfo.headings,
        "links" ->  websiteInfo.links,
        "isLoginPage" -> websiteInfo.isLoginPage
      )
    }

    val websiteInfo = WebsiteInfo(website, title, version.get, headings, linksMap, loginPage)

    val info = Json.toJson(websiteInfo)
    Ok(info);
  }

  /**
    * Match the HTML DOCTYPE and retrieve the version of the document using regex and pattern matching
    * @param docType
    * @return Version of HTML document
    */
  def doctTypeMatch(docType: String): Option[String] = docType match {
    case "(?i)<!doctype\\s*html>" => Some("HTML5")
    case "(<\\?.*?\\?>)?\\s*<!DOCTYPE\\s+([a-zA-Z_][a-zA-Z0-9]*\\s+[a-zA-Z_][a-zA-Z0-9]*\\s+\"[^\"]*\")[^>]*>" => {
      val pattern = "HTML\\s+[0-9]+.?[0-9]+[0-9]*".r
      Some(pattern.findFirstIn(docType).get)
    }
    case _ => Some("UNKNOWN")
  }

  /**
    * Populate the map with headings count in the HTML document
    * @param document
    * @return Map of Headings and count
    */
  def populateHeadingsMap(document: Document): scala.collection.mutable.LinkedHashMap[String, Int] = {
    val headingsMap = scala.collection.mutable.LinkedHashMap[String, Int]()

    val h1Idx = detectHeadings(document, "h1")
    if (h1Idx > 0)
      headingsMap.put("h1", h1Idx)

    val h2Idx = detectHeadings(document, "h2")
    if (h2Idx > 0)
      headingsMap.put("h2", h2Idx)

    val h3Idx = detectHeadings(document, "h3")
    if (h3Idx > 0)
      headingsMap.put("h3", h3Idx)

    val h4Idx = detectHeadings(document, "h4")
    if (h4Idx > 0)
    headingsMap.put("h4", h4Idx)

    val h5Idx = detectHeadings(document, "h5")
    if (h5Idx > 0)
    headingsMap.put("h5", h5Idx)

    val h6Idx = detectHeadings(document, "h6")
    if (h6Idx > 0)
    headingsMap.put("h1", h6Idx)

    headingsMap
  }

  /**
    * Detect headings in the HTML document
    * @param document
    * @param heading
    * @return Headings count
    */
  def detectHeadings(document: Document, heading: String): Int = {
    val headings = document.select(heading)
    headings.size
  }

  /**
    * Detect hyperlinks in the HTML document
    * @param website
    * @param links
    * @param linkType
    * @param attr
    * @return Map of Hyperlinks and count
    */
  def detectLinks(website: String, links: Elements, linkType: String, attr: String): scala.collection.mutable.LinkedHashMap[String, Int] = {
    val linksMap = scala.collection.mutable.LinkedHashMap[String, Int]()
    val hyperlinks = links.asScala

    val websiteURL = new URL(website)
    val host = websiteURL.getHost

    var internalLinksCount = 0;
    var externalLinksCount = 0;

    hyperlinks.map { link =>
      val linkAttr = link.attr(attr)

      if (Utils.isHttpProtocol(linkAttr).get) {
        val linkUrl = new URL(linkAttr);

        if (linkUrl.toExternalForm.matches("^(http(s)?(:\\/\\/))?(www\\.)?[a-zA-Z0-9-_\\.]+"+host+"/([-a-zA-Z0-9:%_\\+.~#?&//=]*)/?[a-zA-Z]+[.]?[a-zA-Z]?")) {
          internalLinksCount += 1
        } else {
          externalLinksCount += 1
        }
      } else {
        internalLinksCount += 1
      }
    }
    linksMap.put("Internal " + linkType, internalLinksCount)
    linksMap.put("External " + linkType, externalLinksCount)

    linksMap
  }

  /**
    * Retrieve all the links such as anchor, image, stylesheet, script from the HTNL document. Links are populated in a map after removing
    * duplicates. Call detectLinks function to perform the link detection.
    * @param website
    * @param document
    * @return Map of Links and count
    */
  def detectPageLinks(website: String, document: Document): scala.collection.mutable.LinkedHashMap[String, Int] = {

    var linksMap = scala.collection.mutable.LinkedHashMap[String, Int]()

    var attr = "href"
    val links = Utils.removeDuplicateElements(document.select("a[href]"), attr)
    linksMap ++= detectLinks(website, links, "Hyperlinks", attr)

    val media = Utils.removeDuplicateElements(document.select("img[src]"), attr)
    linksMap ++= detectLinks(website, media, "Media", attr)
    attr = "src"

    attr = "href"
    val imports = Utils.removeDuplicateElements(document.select("link[href]"), attr)
    linksMap ++= detectLinks(website, imports, "Imports", attr)

    attr = "src"
    val scripts = Utils.removeDuplicateElements(document.select("script[src]"), attr)
    linksMap ++= detectLinks(website, scripts, "Scripts", attr)

    linksMap
  }

  /**
    * Check whether login page
    * @param document
    * @return Is login page
    */
  def isLoginPage(document: Document): Boolean = {

    var isLoginPageAvailable = false
    var isPasswordAvailable = false

    val inputElements = document.body.getElementsByTag("input").asScala

    /* If Password element index > 1, might be registration or sign up page */
    var passwordElementIdx = 0;

    inputElements.map { input =>
      val inputType = input.attr("type").toLowerCase

      if (inputType == "password") {
        isPasswordAvailable = true
        passwordElementIdx += 1
      }
    }

    val usernames = document.body.getElementsContainingText("username")
    val emails = document.body.getElementsContainingText("email")
    val emailAddresses = document.body.getElementsContainingText("email address")
    val forgotPasswords = document.body.getElementsContainingText("Forgot password")
    val signIns = document.body.getElementsContainingText("Sign In")
    val signins = document.body.getElementsContainingText("Signin")
    val logIns = document.body.getElementsContainingText("Log In")
    val logins = document.body.getElementsContainingText("Login")
    val phones = document.body.getElementsContainingText("Phone")
    val signups = document.body.getElementsContainingText("Signup")
    val signUps = document.body.getElementsContainingText("Sign Up")
    val registrations = document.body.getElementsContainingText("Register")

    if (isPasswordAvailable && passwordElementIdx == 1 && (usernames !=  null || emails != null || emailAddresses != null || forgotPasswords != null || signIns != null || signIns != null || logIns != null || logIns != null || phones != null || signups != null || signUps != null || registrations != null)) {
      if (signups.size() > 0 || signUps.size() > 0 || registrations.size() > 0 && forgotPasswords.size == 0) {
        //Signup or Registration page
        isLoginPageAvailable = false
      } else if (signups.size() == 0 || signUps.size() == 0 || registrations.size() == 0 && forgotPasswords.size > 0) {
        //Login Page
        isLoginPageAvailable = true
      } else if ((signups.size() > 0 || signUps.size() > 0 || registrations.size() > 0) && (usernames.size > 0 || emails.size > 0 || emailAddresses.size > 0 || forgotPasswords.size > 0 || signIns.size > 0 || signins.size > 0 || logIns.size > 0 || logins.size > 0 || phones.size > 0))  {
        //Signup page and Login page
        isLoginPageAvailable = true
      } else if (signups.size() > 0 || signUps.size() > 0 || registrations.size() > 0){
        //Signup page
        isLoginPageAvailable = false
      } else {
        //Not a Signup page or Login page
        isLoginPageAvailable = false
      }
    }
    isLoginPageAvailable
  }

}

/**
  * Companion object to wrap form object
  */
object HomeController {
  /**
    * Form object in the Index page
    */
  val httpUrlForm = Form(
    single(
      "website" -> text
    )
  )
}
