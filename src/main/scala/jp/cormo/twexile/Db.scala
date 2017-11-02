package jp.cormo.twexile

import slick.jdbc.JdbcBackend._

object Db {
  val db: DatabaseDef =
    Database.forURL(
      "jdbc:postgresql:mastodon_production",
      null, "org.postgresql.Driver"
    )
  def apply(): DatabaseDef = db
}
