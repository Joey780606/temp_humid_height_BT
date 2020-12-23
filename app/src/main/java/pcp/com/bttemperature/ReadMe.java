package pcp.com.bttemperature;

class ReadMe {
    /*
    0. 重要地方請下註解 // 91 important
    開發心得:
    1. Implement的library
     a. net.danlew:android.joda - 關於時間的處理
     b. com.androidplot:androidplot-core:1.5.7 -
      畫圖用的,但此版是使用更前面的版本,有很多功能已不被新版支援,所以要用那個library畫圖,還要研究
      參考:
       (1). AndroidPlot詳細功能介紹
         (a). https://www.javadoc.io/doc/com.androidplot/androidplot-core/1.5.4/com/androidplot/xy/XYGraphWidget.html#getGridBackgroundPaint
         (b). https://www.javadoc.io/doc/com.androidplot/androidplot-core/1.5.4/com/androidplot/xy/XYPlot.html
       (2). androidplot 與 MPAndroidChart 畫圖表api比較(2018年的資料)
         https://codix.io/repo/1158/similar
     c. 開發者有使用com.parse 這個library,但是詳細使用方式好像不太確定,所以我只拿必要的程式

    2. 只是測試用,目前只要Scan到一台就直接連線(掃到Sensor,UI會跑掉,所以先這樣做)
    3. 發現 jadx會有一些程式無法反組譯出來,目前解法是參考
      https://zpspu.pixnet.net/blog/post/330274885-android-%E5%8F%8D%E7%B7%A8%E8%AD%AF%E5%B7%A5%E5%85%B7%E6%95%99%E5%AD%B8-dex2jar-%E5%92%8Cjd-gui
      使用dex2jar 解出來後(可以解出jadx解不出的地方), 用jd-gui 來看,雖然資料是比較亂,但還解的出來
     */
}
