package jp.cormo.twexile

import com.twitter.finagle.Http
import com.twitter.util.Await
import io.finch._

object RouteApp extends App {
  val printFilter = new PrintFilter

  val challenge = get(/ :: param("hub.challenge")) { challenge: String =>
    Ok(challenge)
  }

  val server = Http
    .server
    .serve("0.0.0.0:80", printFilter.andThen((challenge :+: Subscribe).toServiceAs[Text.Plain]))
  Await.ready(server)
}

class Challenge(val c: String) {
  override def toString: String = c
}