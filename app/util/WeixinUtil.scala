package util

import common.AlarmCachedConifg

/**
 * Created by davihe on 15-9-7.
 */
object WeixinUtil {

  def sendMsg(key: String, msg: String): Unit = {
    if (AlarmCachedConifg.registerAlarm(key, System.currentTimeMillis())) {
      val mmmm = "[test]" + msg
      /*val mm = URLEncoder.encode(mmmm.toString(), "UTF-8");
      //val mm = URLEncoder.encode(msg.toString(), "UTF-8");
      val client = new HttpClient();
      val method = new PostMethod(SystemCachedConstant.getProperties("weixin.http.url"));
      method.addParameter("content", mm);
      val result = client.executeMethod(method);*/
      val result = "200"
      Logger.debug("send msg " + msg + " to weixin ,response " + result)
    } else {
      Logger.debug("resend msg " + msg + " to weixin")
    }
  }

}
