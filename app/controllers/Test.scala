/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the GNU Lesser General Public License version 2.1 (LGPLv2.1) (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
  * @author Abhilash Krishnan 
  * @since 24-05-2017
  */

class Test {

}

object Test {
  def main(args: Array[String]) {
    val p = "([^\\s]+(\\.(?i)(pdf|doc|docx|ppt|pptx|xls|xlsx|epub|odt|odp|ods|swx|ps|rtf|txt|djvu|djv|zip|gzip|tar|gz|rar|bz2|z|tiff|tif|swf|bmp|php|asp|jsp))$)"

    val s = "/test.pdf"

    println(s.matches(p))
  }
}