# FlMML-for-Android
[FlMML](http://flmml.codeplex.com/)のAndroid移植版です。
Javaで記述されており、NDKは使っていません。
> FlMMLはFlashで音楽を鳴らすためのライブラリです。<br>
MML記法のテキストを渡すだけでメロディを奏でることができます。<br>
FlMMLは修正BSDライセンスで公開しています。

### 動作環境
**Android 1.6** \(Donut, api level 4\) 以上<br>
ただし、古い機種でそのまま実行すると`OutOfMemoryError`を投げられるでしょう。

### Instant Runについて
開発の際、Instant Runが有効だとアプリの動作がとても遅くなると思います。<br>
無効にすることをおすすめします。

### 現状の問題点など
* `#WAV9 0`だけとか、`#WAV10 0`だけを書くと落ちる
* スレッドを終了させていないところがある
* 同期しきれていない部分がある

***
###### _FlMML-for-Androidの殆どは[FlMML](http://flmml.codeplex.com/)の実装をもとに書かれています。ありがとうございます。_