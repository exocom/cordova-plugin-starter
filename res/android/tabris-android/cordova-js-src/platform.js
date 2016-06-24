/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

module.exports = {
  id: 'android',
  bootstrap: function() {
    tabris.load(function() {
      /*globals cordova: false */
      tabris.registerType("cordova.plugin", {
        _properties: {
          service: "any"
        },
        _events: {
          finish: true
        }
      });
      cordova.require("cordova/channel").onNativeReady.fire();
      tabris._setEntryPoint(function(loadMain) {
        tabris.app.on("pause", function() {
          cordova.fireDocumentEvent("pause");
        });
        tabris.app.on("resume", function() {
          cordova.fireDocumentEvent("resume");
        });
        document.addEventListener("deviceready", loadMain, false);
      });
    });
  }
};
