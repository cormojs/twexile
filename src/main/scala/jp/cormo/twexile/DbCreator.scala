package jp.cormo.twexile

import slick.dbio.DBIOAction
import scala.concurrent.duration.Duration
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await

object DbCreator extends App {
  val s = Tables.TwexileTokens.schema
  val a = Db().run(DBIO.seq(
    s.drop,
    s.create
  ))
  Await.result(a, Duration.Inf)
}
