package jp.cormo.twexile.endpoint

import io.finch.Endpoint.Result
import io.finch._
import jp.cormo.twexile.{Db, Tables}
import slick.jdbc.PostgresProfile.api._
import twitter4j.TwitterFactory
import twitter4j.auth.RequestToken
import twitter4j.conf.{ConfigurationBuilder, ConfigurationContext}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object AuthorizeCallback extends Endpoint[Unit] {
  override def apply(input: Input): Result[Unit] = get(
    / :: "callback" :: string :: param("oauth_verifier")
  ) {
    process _
  } apply input

  def process(hash: String, verifier: String): Output[Unit] = {
    val query = for {
      token <- Tables.TwexileTokens.filter(_.hash === hash)
    } yield token

    val token = Await.result(Db().run(query.result), Duration.Inf)(0)
    val p = ConfigurationContext.getInstance()
    val cb = new ConfigurationBuilder
    cb.setOAuthConsumerKey(p.getOAuthConsumerKey)
    cb.setOAuthConsumerSecret(p.getOAuthConsumerSecret)
    cb.setDebugEnabled(true)

    val twitter = new TwitterFactory(cb.build).getInstance
    val accessToken = twitter.getOAuthAccessToken(new RequestToken(
      token.twitterRequestToken.get,
      token.twitterRequestTokenSecret.get
    ), verifier)
    val res = Await.result(Db().run(
      Tables.TwexileTokens
        .filter(_.hash === hash)
        .map(t => (t.twitterAccessToken, t.twitterAccessSecret))
        .update((Some(accessToken.getToken), Some(accessToken.getTokenSecret)))
    ), Duration.Inf)

    if (res == 0)
      Unauthorized(new IllegalArgumentException)
    else
      Ok(())
  }
}
