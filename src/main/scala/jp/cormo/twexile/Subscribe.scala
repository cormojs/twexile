package jp.cormo.twexile

import io.finch._
import twitter4j.TwitterFactory

import scala.util.parsing.combinator.RegexParsers
import scala.xml.XML

object Subscribe extends Endpoint[Unit] {
  private val yourName = "xxxx"
  def apply(): Endpoint[Unit] = post(/ :: stringBody) { process _ }
  override def apply(input: Input) = apply().apply(input)

  private def process(body: String): Output[Unit] = {
    val xml = XML.loadString(body)
    val name = xml \ "author" \ "name"
    val content = xml \ "entry" \ "content"
    val id = xml \ "entry" \ "id"
    object ContentExtractor extends RegexParsers {
      def apply(input: String): Option[String] = {
        val pat = """<p>""".r ~ """[^<]+""".r ~ """<\/p>""".r ^^ {
          case _ ~ str ~ _ => str
        }


        parseAll(pat, input) match {
          case Success(str, _) => Some(str)
          case _ => None
        }
      }
    }

    ContentExtractor.apply(content.text).foreach({ post =>
      if (name.text == yourName) {
        TwitterFactory.getSingleton.updateStatus(s"""${post} [亡命先  ${id.text}]""")
      }
    })

    NoContent[Unit]
  }
}
