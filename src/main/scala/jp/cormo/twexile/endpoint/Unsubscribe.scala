package jp.cormo.twexile.endpoint

import java.net.{URI, URLEncoder}

import com.twitter.finagle.Http
import com.twitter.finagle.http.{Method, Request, Status}
import io.finch._
import jp.cormo.twexile.{Db, Tables}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import com.twitter.util.{Await => Twait}

object Unsubscribe extends Endpoint[Unit] {
  def apply(): Endpoint[Unit] = get(/ :: "unsubscribe" :: param("token")) { process _ }
  override def apply(input: Input) = apply().apply(input)

  def process(token: String): Output[Unit] = {
    val result = Await.result(
      Db().run(
        Tables.OauthAccessTokens
          .filter(_.token === token)
          .map(_.resourceOwnerId).result
      ).flatMap(id => {
        Db().run(
          Tables.Users.filter(_.id === id(0).get).map(_.accountId).result
        )
      }).flatMap(accountId => {
        Db().run(
          Tables.Accounts.filter(_.id === accountId(0)).map(_.username).result
        )
      }), Duration.Inf)

    if (result.isEmpty) {
      BadRequest(new IllegalArgumentException)
    } else {
      val username = result(0)
      val topic = URLEncoder.encode(s"https://nayukana.info/users/$username.atom", "UTF-8")
      val callback = URLEncoder.encode("https://twexile.nayukana.info/", "UTF-8")
      val url = new URI(s"/api/push?hub.mode=unsubscribe&hub.topic=$topic&hub.callback=$callback")


      val client = Http.client
        .withTransport
        .tls("nayukana.info")
        .newService("nayukana.info:443")

      val req = Request(Method.Post, url.toString)

      Twait.ready(client(req))

      NoContent[Unit]
    }
  }
}
