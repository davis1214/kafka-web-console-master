package com.sohu.alarm

import java.net.URLEncoder


/**
 * Created by davihe on 15-9-7.
 */
object WeixinDemo {

  /* WEIXIN_JAR="/usr/local/src/adetl/sh/check/weixin-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
   WEIXIN_APP_CLASS="com.sohu.monitor.SendHttpMsg"
   java -cp $WEIXIN_JAR $WEIXIN_APP_CLASS $msg*/

  def main(args: Array[String]) {
    val url = "http://10.10.79.155:8011";
    val msg = new StringBuffer();

    /* if (args.length == 0)
       System.err.println("请输入要发送消息的内容");
     else {
       args.map(msg.append(_ + " "))
     }*/

    msg.append("test in kafka-web-console")
    val mm = URLEncoder.encode(msg.toString(), "UTF-8");

    val client = new HttpClient();
    val method = new PostMethod(url);
    method.addParameter("content", mm);
    client.executeMethod(method);
    println("ddd ")

  }

}
