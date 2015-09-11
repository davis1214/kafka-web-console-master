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

app.controller("SettingsController", function ($http, $scope, $location) {
    $('input').tooltip()

    $http.get('settings.json').success(function (data) {
        $scope['allSettings'] = data;
    });

    /* $scope.updateSettings = function (settings) {
     $http.post('settings.json', [
     { key: 'PURGE_SCHEDULE', value: settings.purgeSchedule},
     { key: 'OFFSET_FETCH_INTERVAL', value: settings.offsetFetchInterval.toString()}
     ]).success(function () {
     $location.path("/");
     });
     };*/

    $scope.getSettings = function (type) {
        $http.get('getSettings.json').
            success(function (data) {
                $scope[type + 'Settings'] = data;
            });
    };

    $scope.getAlarms = function (type) {
        $http.get('getAlarms.json').
            success(function (data) {
                $scope[type + 'Alarms'] = data;
            });
    };

});