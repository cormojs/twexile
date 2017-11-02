package jp.cormo.twexile.endpoint

import java.util.concurrent.Future

import com.twitter.finagle.http.Status
import io.finch.Endpoint.Result
import io.finch._
import jp.cormo.twexile.{Db, Tables}
import slick.jdbc.PostgresProfile.api._
import twitter4j.TwitterFactory
import twitter4j.conf.{ConfigurationBuilder, ConfigurationContext}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import scala.concurrent.ExecutionContext.Implicits.global

object Authorize extends Endpoint[String] {
  def apply(): Endpoint[String] = get(/ :: "authorize" :: param("token")) { process _ }
  override def apply(input: Input): Result[String] = apply().apply(input)

  def process(token: String): Output[String] = {
    val p = ConfigurationContext.getInstance()
    val cb = new ConfigurationBuilder
    cb.setOAuthConsumerKey(p.getOAuthConsumerKey)
    cb.setOAuthConsumerSecret(p.getOAuthConsumerSecret)
    cb.setDebugEnabled(true)

    val twitter = new TwitterFactory(cb.build).getInstance

    val hash = scala.util.Random.alphanumeric.take(26).mkString
    val reqToken = twitter.getOAuthRequestToken(s"https://twexile.nayukana.info/callback/${hash}")

    Await.ready(Db().run(deleteNotAuthorized), Duration.Inf)

    val notAuthorized = Await.result(
      Db().run(
        (for {
          ownerId <- Tables.OauthAccessTokens.filter(_.token === token).map(_.resourceOwnerId)
          twexileToken <- Tables.TwexileTokens.filter(_.resourceOwnerId === ownerId)
        } yield twexileToken).result
      ), Duration.Inf)

    if (notAuthorized.isEmpty) {
      val update = Await.result(
        Db().run(
          (for {
            matched <- Tables.OauthAccessTokens.filter(_.token === token)
          } yield matched.resourceOwnerId).result
        ).flatMap(ownerId => Db().run(
          Tables.TwexileTokens.insertOrUpdate(
            Tables.TwexileTokensRow(
              0, hash, ownerId(0).get, None, None,
              Some(reqToken.getToken), Some(reqToken.getTokenSecret)
            )
          )
        )), Duration.Inf)


      if (update == 1) {
        Ok(reqToken.getAuthenticationURL)
      } else {
        BadRequest(new IllegalArgumentException)
      }
    } else {
      Ok("")
    }
  }

  def deleteNotAuthorized = Tables.TwexileTokens.filter(_.twitterAccessToken.isEmpty).delete

}
