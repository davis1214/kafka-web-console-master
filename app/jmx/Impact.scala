package jmx

/** @author Stephen Samuel */
sealed trait Impact

object Impact {

  case object Info extends Impact

  case object Action extends Impact

  case object ActionInfo extends Impact

  case object Uknown extends Impact

}
