# Twexile for Mastodon
## Concept
Mastodonへの投稿をTwitterに投げるWebアプリ。近日中に [なゆかな](https://nayukana.info) にて公開予定。

Web app which subscribe your Mastodon and post them to your twitter.
Twexile will be available at [NAYUKANA](https://nayukana.info) soon.

## How to use
1. twitter4j.propertiesを作成しConsumer Key & SecretおよびAccess Token & Secretの設定をする
2. `Subscribe.scala`の10行目`yourName`を自分のMastodon IDにする
3. `RouteApp.scala`の16行目、ポート80番になってるので適宜変える
4. コンパイル、デプロイ
5. Mastodonインスタンスに購読リクエストを送る e.g;
```
# curl -X POST -Ss https://${YOUR_MASTODON_DOMAIN}/api/push -d "hub.mode=subscribe" -d "hub.topic=https://${YOUR_MASTODON_DOMAIN}/users/${YOUR_NAME}.atom" -d "hub.callback=http://${YOUR_TWEXILE_SERVER}/"
```
6. Mastodonに投稿するとTwitterにも投稿される prefixは[亡命先 ${URL}]