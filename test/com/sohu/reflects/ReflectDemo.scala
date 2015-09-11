package com.sohu.reflects

/**
 * Created by davihe on 15-9-10.
 */
object ReflectDemo {

  def main(args: Array[String]) {
    // println("-->" + models.BrokerHistoryInfo.getClass.getDeclaredField())

    models.BrokerHistoryInfo.getClass.getFields.map(f => {
      println("->" + f.getName)
    })
    models.BrokerHistoryInfo.getClass.getDeclaredFields.map(f => {
      println("--->" + f.getName)
    })


  }

}
