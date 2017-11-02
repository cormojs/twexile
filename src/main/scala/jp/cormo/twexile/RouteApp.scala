package jp.cormo.twexile

import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._
import jp.cormo.twexile.endpoint._
import com.twitter.finagle.http.filter.Cors

object RouteApp extends App {
  val printFilter = new PrintFilter
  val policy: Cors.Policy = Cors.Policy(
    allowsOrigin = _ => Some("*"),
    allowsMethods = _ => Some(Seq("GET", "POST")),
    allowsHeaders = _ => Some(Seq("Accept"))
  )

  val challenge = get(/ :: param("hub.challenge")) { challenge: String =>
    Ok(challenge)
  }

  val endpoint =
    challenge :+: SubscribeCallback :+: Authorize :+: AuthorizeCallback :+: Subscribe :+: Unsubscribe

  val server = Http
    .server
    .serve("0.0.0.0:8080",
      new Cors.HttpFilter(policy)
        .andThen(printFilter
          .andThen(endpoint.toServiceAs[Text.Plain])))
  Await.ready(server)
}

class Challenge(val c: String) {
  override def toString: String = c
}