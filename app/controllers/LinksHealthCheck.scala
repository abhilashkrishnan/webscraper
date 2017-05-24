package controllers

import java.net._
import java.util.concurrent.CountDownLatch

import javax.inject._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.i18n.Messages.Implicits._

import org.jsoup._
import org.jsoup.nodes._
import org.jsoup.select._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Wrapper class for Health of Link
  *
  * @param link
  * @param statusCode
  * @param statusMessage
  * @param active
  */
case class LinkHealth (link: String, statusCode: Int, statusMessage: String, active: Boolean) {
  override def equals(that: Any): Boolean =
    that match {
      case that: LinkHealth => that.canEqual(this) && this.hashCode == that.hashCode
      case _ => false
    }
  override def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + statusCode;
    result = prime * result + (if (link == null) 0 else link.hashCode)
    result = prime * result + (if (statusMessage == null) 0 else statusMessage.hashCode)
    result = prime * result + (if (active == false) 0 else active.hashCode)
    return result
  }
}

/**
  * This controller creates an `Action` to handle Health Check of Links requests to the
  * application's home page.
  */
@Singleton
class LinksHealthCheck @Inject() extends Controller {

  /**
    * Health Check action of links in a HTML document
    *
    * @return Health Check list as Json Array
    */
  def healthCheck() = Action { implicit request =>
    def values = HomeController.httpUrlForm.bindFromRequest.data
    def website = values("website")
    val document = Jsoup.connect(website).get
    val linksHealthBuf = detectLinksHealth(website, document)

    implicit val linksHealthWrites = new Writes[LinkHealth] {
      def writes(linkHealth: LinkHealth) = Json.obj(
        "link" -> linkHealth.link,
        "statusCode" -> linkHealth.statusCode,
        "statusMessage" -> linkHealth.statusMessage,
        "active" -> linkHealth.active
      )
    }

    val linksHealth = Json.toJson(linksHealthBuf)

    Ok(linksHealth)
  }

  /**
    * Checks if supplied link is a prepended http(s)
    *
    * @param url
    * @return Whether http(s) url
    */
  def isHttpDirectLink(url: String): Option[Boolean] = {
    var isHttp: Option[Boolean] = Some(false)

    if (url.matches("^((http)+(s)?(:\\/\\/))+(www\\.)?[a-zA-Z0-9\\?=\\-_\\.\\,\\&\\/\\/\\/\\/p{L}]*([-a-zA-Z0-9:%_\\+.~#?\\&\\=]*)?")){
      isHttp = Some(true)
    }

    isHttp
  }


  /**
    * Checks if supplied link is supported protocol using regex and pattern matching. Only http(s) protocol is supported
    *
    * @param url
    * @return Whether unsupported url
    */
  def unsupportedProtocol(url: String): Option[Boolean] = {
    var unSupported: Option[Boolean] = Some(false)
    val ftpPattern = "^((ftp)+(:\\/\\/))+(www\\.)?[a-zA-Z0-9\\?=\\-_\\.\\,\\&\\/\\/\\/\\/p{L}]*([-a-zA-Z0-9:%_\\+.~#?\\&\\=]*)?"

    if (url.matches(ftpPattern)){
      unSupported = Some(true)
    }

    unSupported
  }

  def unsupportedFileExtensions(url: String): Option[Boolean] = {
    var unSupported: Option[Boolean] = Some(false)
    val filePattern = "([^\\s]+(\\.(?i)(pdf|doc|docx|ppt|pptx|xls|xlsx|epub|odt|odp|ods|swx|ps|rtf|txt|djvu|djv|zip|gzip|tar|gz|rar|bz2|z|tiff|tif|swf|bmp|php|asp|jsp))$)"

    if(url.matches(filePattern)) {
      unSupported = Some(true)
    }

    unSupported
  }
  /**
    * Generate LinkHealth object
    *
    * @param link
    * @param response
    * @param active
    * @return LinkHealth object
    */
  def genLinkHealth(link: String, response: Connection.Response, active: Boolean): Option[LinkHealth] = {
    Some(LinkHealth(link, response.statusCode, response.statusMessage, active))
  }

  /**
    * Retrieve LinkHealth object on connection to the link. Connection success and failure conditions are handled.
    *
    * @param link
    * @return LinkHealth object
    */
  def getLinkHealth(link: String): Option[LinkHealth] = {
    var linkHealth: Option[LinkHealth] = None
    val response = getConnectionResponse(link)
    val statusCode = response.statusCode

    if (statusCode == 200) {
      linkHealth = genLinkHealth(link, response, true)
    } else {
      linkHealth = genLinkHealth(link, response, false)
    }
    linkHealth
  }

  /**
    * Check the health of the links from the Jsoup Element. Executes asynchronously using Scala future
    *
    * @param url
    * @param e
    * @param attr
    * @return Future containing LinkHealth object
    */
  def checkHealth(url: String, e: Element, attr: String): Future[LinkHealth] = Future {
    var linkHealth: Option[LinkHealth] = None

    val linkAttr = e.attr(attr)
    val linkUrl = new URL(url)

    if (linkAttr != null && !linkAttr.equals("")) {
      //Progress indicator on the console
      println("->")

      try {
        if (unsupportedFileExtensions(linkAttr).get) {
          linkHealth = Some(LinkHealth(linkAttr, 415 , "DOCUMENT FORMAT IS NOT SUPPORTED", false))
        }
        else if (isHttpDirectLink(linkAttr).get) {
          linkHealth = getLinkHealth(linkAttr)
        } else if (unsupportedProtocol(linkAttr).get) {
          linkHealth = Some(LinkHealth(linkAttr, 415 , "PROTOCOL IS NOT SUPPORTED", false))
        } else {
          var generatedLink: Option[String] = None

          if (linkAttr.matches("^(\\/\\/)+([a-zA-Z0-9\\?=:-_\\,\\.\\&\\/\\/\\/\\\\&p{L}])+([-a-zA-Z0-9:%_\\+.~#?&//=]*)?")) {
            //Link starts with DOUBLE SLASH OR SINGLE SLASH
            generatedLink = Some(linkUrl.getProtocol + ":" + linkAttr)
            linkHealth = getLinkHealth(generatedLink.get)
          } else if (linkAttr.matches("^(\\/)+([a-zA-Z0-9\\?=:-_\\.\\,\\&\\/\\/\\/\\\\&p{L}])+([-a-zA-Z0-9:%_\\+.~#?&//=]*)?")) {
            //Link starts with SINGLE SLASH
            generatedLink = Some(linkUrl.getProtocol + "://" + linkUrl.getHost + linkAttr)
            linkHealth = getLinkHealth(generatedLink.get)
          }
          else if (linkAttr.matches("^([a-zA-Z0-9\\?=:\\-_\\,\\.\\/\\/\\/\\/\\&p{L}])+.?([a-zA-Z])*")) {
            //Link starts with NO SLASHES
            generatedLink = Some(linkUrl.getProtocol + "://" + linkUrl.getHost + "/" + linkAttr)
            linkHealth = getLinkHealth(generatedLink.get)
          } else if (linkAttr.matches("^(javascript|JavaScript|JAVASCRIPT)?:?([a-zA-Z0-9()])+;?")) {
            //JavaScript functions
            linkHealth = Some(LinkHealth(linkAttr, 415, "JAVASCRIPT NOT SUPPORTED", false))
          }else if (linkAttr.matches("^(mailto)?:?([a-zA-Z0-9()\\@\\.\\/\\/\\/\\=?\\s\\&])+")) {
            //Email links
            linkHealth = Some(LinkHealth(linkAttr, 415, "EMAIL NOT SUPPORTED", false))
          } else if (linkAttr.matches("^(\\#)+([a-zA-Z0-9()\\/?\\-_\\,\\.\\/\\/\\/\\p{L}\\&])*")) {
            //HASHBANG
            generatedLink = Some(linkUrl.toExternalForm + linkAttr)
            linkHealth = getLinkHealth(generatedLink.get)
          } else {
            linkHealth = Some(LinkHealth(linkAttr, 500, "UNSUPPORTED OPERATION - PROBABLY TOO MANY REDIRECTS", false))
          }
        }
      } catch {
        case ex: HttpStatusException => {
          linkHealth = Some(LinkHealth(linkAttr, ex.getStatusCode, ex.getMessage, false))
        }
        case ex: Exception => {
          linkHealth = Some(LinkHealth(linkAttr, 500, "CONNECTION FAILED", false))
        }
      }
    }
    linkHealth.get
  }


  /**
    * Checks if the LinkHealth object is available in the list
    *
    * @param linksHealthBuff
    * @param linkHealth
    * @return If LinkHealth object is available
    */
  def checkInBuffer(linksHealthBuff: ListBuffer[LinkHealth], linkHealth: LinkHealth): Boolean = {
    linksHealthBuff.contains(linkHealth)
  }

  /**
    * Perform the health check of the links. Optimised using Scala future and Java CountDownLatch Concurrency API. Since the future tasks
    * execute asynchronously CountDownLatch await for all the tasks to complete and merge the results from each of the future tasks into
    * a ListBuffer.
    *
    * @param website
    * @param linksHealthBuff
    * @param links
    * @param attr
    * @return List of LinkHealth objects
    */
  def linksHealthCheck(website: String, linksHealthBuff: ListBuffer[LinkHealth], links: Elements, attr: String): ListBuffer[LinkHealth] = {
    var hyperLinksHealthBuff = new ListBuffer[LinkHealth]()

    val latch = new CountDownLatch(links.size())
    val hyperlinks = links.asScala

    hyperlinks.map { link =>
      try {
        checkHealth(website, link, attr) onComplete {
          case Success(result) => {
            if (!checkInBuffer(linksHealthBuff, result)) {
              hyperLinksHealthBuff += result
            }
            latch.countDown()
          }
          case Failure(e) => {
            e.printStackTrace()
            latch.countDown()
          }
        }
      } catch {
          case ex: Exception => {
            //Nothing to be done. Fatal error
            latch.countDown()
          }
      }
  }
  latch.await()
  hyperLinksHealthBuff
}

  /**
    * Retrieve all the links form website after making connection and remove duplicates and populates into a ListBuffer and
    * send the links for health check.
    *
    * @param website
    * @param document
    * @return List of LinkHealth objects
    */
  def detectLinksHealth(website: String, document: Document): ListBuffer[LinkHealth] = {
    var linksHealthBuff = new ListBuffer[LinkHealth]()

    /* Detect <a> Links */
    var attr = "href"
    val links = Utils.removeDuplicateElements(document.select("a[href]"), attr)
    linksHealthBuff ++= linksHealthCheck(website, linksHealthBuff, links, attr)

    /* Detect Images */
    attr = "src"
    val media = Utils.removeDuplicateElements(document.select("img[src]"), attr)
    linksHealthBuff ++= linksHealthCheck(website, linksHealthBuff, media, attr)

    /* Detect stylesheets and favicon*/
    attr = "href"
    val imports = Utils.removeDuplicateElements(document.select("link[href]"), attr)
    linksHealthBuff ++= linksHealthCheck(website, linksHealthBuff, imports, attr)

    /* Detect Scripts*/
    attr = "src"
    val scripts = Utils.removeDuplicateElements(document.select("script[src]"), attr)
    linksHealthBuff ++= linksHealthCheck(website, linksHealthBuff, scripts, attr)

    linksHealthBuff

  }

  /**
    * Retrieve the HTTP Connection response after making connection to the link
    *
    * @param link
    * @return HTTP Connection response
    */
  def getConnectionResponse(link: String): Connection.Response = {
    var response: Option[Connection.Response] = None
    val connection = Jsoup.connect(link)

    if (connection != null) {
      connection.ignoreContentType(true)
      connection.validateTLSCertificates(false)
      connection.get
      response = Some(connection.response)
    }
    response.get
  }
}

