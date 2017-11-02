package jp.cormo.twexile.endpoint

import io.finch.{post, _}
import jp.cormo.twexile.{Db, Tables}
import slick.jdbc.PostgresProfile.api._
import twitter4j.TwitterFactory
import twitter4j.conf.{ConfigurationBuilder, ConfigurationContext}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.parsing.combinator.RegexParsers
import scala.xml.XML

object SubscribeCallback extends Endpoint[Unit] {
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
      println("1")
      val accessToken = Await.result(Db().run(
        (for {
          account <- Tables.Accounts.filter(_.username === name.text)
          user <- Tables.Users.filter(_.accountId === account.id)
          twitterToken <- Tables.TwexileTokens if twitterToken.resourceOwnerId === user.id
        } yield twitterToken).result
      ), Duration.Inf)(0)
      println("2")
      val p = ConfigurationContext.getInstance()
      val cb = new ConfigurationBuilder
      cb.setOAuthConsumerKey(p.getOAuthConsumerKey)
      cb.setOAuthConsumerSecret(p.getOAuthConsumerSecret)
      cb.setOAuthAccessToken(accessToken.twitterAccessToken.get)
      cb.setOAuthAccessTokenSecret(accessToken.twitterAccessSecret.get)
      cb.setDebugEnabled(true)

      println("3")
      val twitter = new TwitterFactory(cb.build).getInstance
      twitter.updateStatus(s"""${post} [from なゆかな ${id.text}]""")
      println("4")
    })

    NoContent[Unit]
  }
}
