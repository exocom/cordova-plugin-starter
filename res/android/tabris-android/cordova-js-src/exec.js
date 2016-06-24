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

var cordovaBridge = (function() {
  /*globals cordova: false */

  var pluginProxies = {};

  var fakeWindow = function () {

    var object = {};
    var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';

    function InvalidCharacterError(message) {
      this.message = message;
    }
    InvalidCharacterError.prototype = new Error;
    InvalidCharacterError.prototype.name = 'InvalidCharacterError';

    // encoder
    // [https://gist.github.com/999166] by [https://github.com/nignag]
    object.btoa || (
    object.btoa = function (input) {
      var str = String(input);
      for (
        // initialize result and counter
        var block, charCode, idx = 0, map = chars, output = '';
        // if the next str index does not exist:
        //   change the mapping table to "="
        //   check if d has no fractional digits
        str.charAt(idx | 0) || (map = '=', idx % 1);
        // "8 - idx % 1 * 8" generates the sequence 2, 4, 6, 8
        output += map.charAt(63 & block >> 8 - idx % 1 * 8)
      ) {
        charCode = str.charCodeAt(idx += 3/4);
        if (charCode > 0xFF) {
          throw new InvalidCharacterError("'btoa' failed: The string to be encoded contains characters outside of the Latin1 range.");
        }
        block = block << 8 | charCode;
      }
      return output;
    });

    // decoder
    // [https://gist.github.com/1020396] by [https://github.com/atk]
    object.atob || (
    object.atob = function (input) {
      var str = String(input).replace(/=+$/, '');
      if (str.length % 4 == 1) {
        throw new InvalidCharacterError("'atob' failed: The string to be decoded is not correctly encoded.");
      }
      for (
        // initialize result and counters
        var bc = 0, bs, buffer, idx = 0, output = '';
        // get next character
        buffer = str.charAt(idx++);
        // character found in table? initialize bit storage and add its ascii value;
        ~buffer && (bs = bc % 4 ? bs * 64 + buffer : buffer,
          // and if not first of each 4 characters,
          // convert the first 8 bits to one ascii character
          bc++ % 4) ? output += String.fromCharCode(255 & bs >> (-2 * bc & 6)) : 0
      ) {
        // try to find character in table (0-63, not found => -1)
        buffer = chars.indexOf(buffer);
      }
      return output;
    });
    return object;

  }();

  var buildPayload = function(payload, message) {
    var payloadKind = message.charAt(0);
    if (payloadKind == 's') {
      payload.push(message.slice(1));
    } else if (payloadKind == 't') {
      payload.push(true);
    } else if (payloadKind == 'f') {
      payload.push(false);
    } else if (payloadKind == 'N') {
      payload.push(null);
    } else if (payloadKind == 'n') {
      payload.push(+message.slice(1));
    } else if (payloadKind == 'A') {
      var data = message.slice(1);
      var bytes = fakeWindow.atob(data);
      var arraybuffer = new Uint8Array(bytes.length);
      for (var i = 0; i < bytes.length; i++) {
        arraybuffer[i] = bytes.charCodeAt(i);
      }
      payload.push(arraybuffer.buffer);
    } else if (payloadKind == 'S') {
      payload.push(fakeWindow.atob(message.slice(1)));
    } else if (payloadKind == 'M') {
      var multipartMessages = message.slice(1);
      while (multipartMessages !== "") {
        var spaceIdx = multipartMessages.indexOf(' ');
        var msgLen = +multipartMessages.slice(0, spaceIdx);
        var multipartMessage = multipartMessages.substr(spaceIdx + 1, msgLen);
        multipartMessages = multipartMessages.slice(spaceIdx + msgLen + 1);
        buildPayload(payload, multipartMessage);
      }
    } else {
      payload.push(JSON.parse(message));
    }
  };

  var processAndroidEvent = function(event) {
    var message = event.message;
    try {
      var firstChar = message.charAt(0);
      if (firstChar == 'J') {
        eval(message.slice(1));
      } else if (firstChar == 'S' || firstChar == 'F') {
        var success = firstChar == 'S';
        var keepCallback = message.charAt(1) == '1';
        var spaceIdx = message.indexOf(' ', 2);
        var status = +message.slice(2, spaceIdx);
        var nextSpaceIdx = message.indexOf(' ', spaceIdx + 1);
        var callbackId = message.slice(spaceIdx + 1, nextSpaceIdx);
        var payloadMessage = message.slice(nextSpaceIdx + 1);
        var payload = [];
        buildPayload(payload, payloadMessage);
        cordova.callbackFromNative(callbackId, success, status, payload, keepCallback);
      } else {
        console.log("processMessage failed: invalid message: " + JSON.stringify(message));
      }
    } catch (e) {
      console.log("processMessage failed: Error: " + e);
      console.log("processMessage failed: Stack: " + e.stack);
      console.log("processMessage failed: Message: " + message);
    }
  };

  var getPlugin = function(service) {
    var plugin = pluginProxies[ service ];
    if (typeof plugin === "undefined") {
      plugin = tabris.create("cordova.plugin", {service: service});
      plugin.on("finish", function(event) {
        processAndroidEvent(event);
      });
      pluginProxies[ service ] = plugin;
    }
    return plugin;
  };

  return {
    exec: function(service, action, args, callbackId ) {
      var plugin = getPlugin(service);
      var arguments = JSON.stringify(args);
      plugin._nativeCall("exec", {action: action, arguments: arguments, callbackId: callbackId});
    }
  };

}());

function tabrisExec() {
  var successCallback, failCallback, service, action, actionArgs, splitCommand;
  var callbackId = null;
  if (typeof arguments[0] !== "string") {
    // FORMAT ONE
    successCallback = arguments[0];
    failCallback = arguments[1];
    service = arguments[2];
    action = arguments[3];
    actionArgs = arguments[4];
    // Since we need to maintain backwards compatibility, we have to pass
    // an invalid callbackId even if no callback was provided since plugins
    // will be expecting it. The Cordova.exec() implementation allocates
    // an invalid callbackId and passes it even if no callbacks were given.
    callbackId = "INVALID";
  } else {
    // FORMAT TWO, REMOVED
    try {
      splitCommand = arguments[0].split(".");
      action = splitCommand.pop();
      service = splitCommand.join(".");
      actionArgs = Array.prototype.splice.call(arguments, 1);
      console.log(
        "The old format of this exec call has been removed (deprecated since 2.1). Change to: " +
        "cordova.exec(null, null, \"" + service + "\", \"" + action + "\"," + JSON.stringify(actionArgs) + ");"
      );
      return;
    } catch (e) {
    }
  }
  // If actionArgs is not provided, default to an empty array
  actionArgs = actionArgs || [];
  // Register the callbacks and add the callbackId to the positional
  // arguments if given.
  if (successCallback || failCallback) {
    callbackId = service + cordova.callbackId++;
    cordova.callbacks[callbackId] = {success: successCallback, fail: failCallback};
  }
  cordovaBridge.exec(service, action, actionArgs, callbackId);
}

module.exports = tabrisExec;
