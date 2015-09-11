package models


/**
 * Created by davihe on 15-9-10.
 */
object Settings extends Enumeration {

  import Database.settingsTable

  val PurgeTypeBroker = Value("BROKER")
  val PurgeTypeTopic = Value("TOPIC")
  val PurgeTypeAlarm = Value("ALARM")
  val PurgeTypeOther = Value("OTHER")

  implicit object SettingsWrites extends Writes[Settings] {
    def writes(setting: Settings) = {
      Json.obj(
        "purgeType" -> setting.PurgeType,
        "fetchInterval" -> setting.FetchInterval,
        "purgeSchedule" -> setting.PurgeSchedule,
        "isValid" -> setting.IsValid
      )
    }
  }

  def findAll: Seq[Settings] = inTransaction {
    from(settingsTable)(s => where(s.IsValid === 1) select (s)).toList
  }

  def findByPurgeType(purgeType: String): Option[Settings] = inTransaction {
    from(settingsTable)(s => where(s.PurgeType === purgeType and s.IsValid === 1) select (s)).headOption
  }

  def update(setting: Settings) = inTransaction {
    settingsTable.update(setting)
  }
}

case class Settings(PurgeType: String, FetchInterval: String, PurgeSchedule: String, IsValid: Int)
  extends KeyedEntity[Long] {

  override val id: Long = 0

  def toMap: Map[String, Any] = {
    val map = Map("PurgeType" -> PurgeType, "FetchInterval" -> FetchInterval, "PurgeSchedule" -> PurgeSchedule, "IsValid" -> IsValid)
    map
  }

  //override def toString = "%s:%s:%s:%s".format(name, PurgeType, FetchInterval, PurgeSchedule)
}


