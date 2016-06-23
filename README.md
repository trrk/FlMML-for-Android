# FlMML-for-Android
[FlMML](http://flmml.codeplex.com/)のAndroid移植版です。
Javaで記述されており、NDKは使っていません。
> FlMMLはFlashで音楽を鳴らすためのライブラリです。<br>
MML記法のテキストを渡すだけでメロディを奏でることができます。<br>
FlMMLは修正BSDライセンスで公開しています。

### 動作環境
**Android 1.6** \(Donut, api level 4\) 以上<br>
ただし、古い機種でそのまま実行すると`OutOfMemoryError`を投げられるでしょう。

### 現状の問題点など
今後修正していく予定です。
* `#WAV9 0`だけとか、`#WAV10 0`だけを書くと落ちる
* スレッドを終了させていないところがある
* 警告が大量に出るとしばらくUIスレッドが固まる
* マルチスレッド部分が少し怪しい

\(原因不明\)
* GBノイズ\(`@11`\)で`o5c+`が来ると落ちる

***
###### _FlMML-for-Androidの殆どは[FlMML](http://flmml.codeplex.com/)の実装をもとに書かれています。ありがとうございます。_