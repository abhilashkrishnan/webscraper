package controllers

import java.io.Console

import org.jsoup.select.Elements

import scala.collection.JavaConverters._

/**
  * Utility functions
  */

object Utils {
  def isHttpProtocol(url: String): Option[Boolean] = {
    var isHttp: Option[Boolean] = Some(false)

    if (url.startsWith("http") || url.startsWith("https"))
      isHttp = Some(true)
    isHttp
  }

  def removeDuplicateElements(e: Elements, attr: String) : Elements = {
    val elements: Elements = new Elements()
    var eleSet = Set[String]()

    val ele = e.asScala
    ele.map { element =>
      if(!eleSet.contains(element.attr(attr)) && element.attr(attr) != null && !element.attr(attr).equals("")) {
        eleSet += element.attr(attr)
        elements.add(element)
      }
    }
    elements
  }

}

