/*
 * Copyright 2014 Claude Mamo
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package controllers


object Settings extends Controller {

  /*  def update() = Action { request =>
      request.body.asJson match {
        case Some(JsArray(settings)) => {
          updateSettings(settings)
          Ok
        }
        case _ => BadRequest
      }
    }*/

  def index() = Action {
    Ok(Json.toJson(models.Settings.findAll))
  }

  def getSettings() = Action {
    Ok(Json.toJson(models.Settings.findAll))
  }

  def getAlarms() = Action {
    Ok(Json.toJson(models.Alarms.findAll))
  }

  /*def updateSettings(settings : Seq[JsValue]) {
    settings.map { s =>
      models.Settings.update(Settings(s.\("key").as[String], s.\("value").as[String]))
      Akka.system.actorSelection("akka://application/user/router") ! Message.SettingsUpdateNotification
    }
  }*/

}
