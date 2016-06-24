## Veeplay Cordova Plugin

###Overview

The Veeplay Cordova plugin exposes the native functionalities of the Veeplay SDK inside a Cordova application. Currently the iOS and Android platforms are support, while the Windows platform is being considered. If you would like to see Windows or other platforms supported, please drop us a line at office@veeplay.com.

The plugin allows content video units and ads to be rendered by configuring the player using the Veeplay JSON configuration DSL (documentation can be found here: [http://veeplay.github.io/json-docs/]()). The JSON configuration can be passed to the player either as a Javascript object or via an URL linking to a JSON configuration file.

###Instalation

    cordova plugin add https://github.com/veeplay/veeplay-cordova-plugin.git

###Usage and Javascript API

All of the plugins methods are grouped under the veeplay clobber. As such, you can call any method using code similar to:
    
    window.veeplay.<method_name>

####Note on subtitles usage

Currently, SRT and WebVTT subtitles are supported with Veeplay on Android only. WebVTT subtitles inside HLS streams are automatically supported starting with Veeplay 2.1.12. SRT subtitles can be configured by adding a subtitlesUrl property inside a content unit object.

	"content": [
		{
		  "url": "STREAMURL",
		  "subtitlesUrl": "SUBTITLESURL.srt",
		  "autoplay": true
		}
	]

####Playing videos inline (inside an HTML element)

In order to add itself on the screen, when requested to play, the player will look for an HTML element with the "player" ID and will bind itself to this element. Before calling playFromUrl or playFromObject, add such an element to the DOM. Example:

     <object width="225" height="120" style="background-color: #000000; margin-top: 20px;" id="player">

Alternatively, a set of **X,Y,width,height** coordinates can be supplied for positioning and sizing the player container, regardless of the DOM contents. This use-case is further detailed in the documentation of the **playFromUrl** and **playFromObject** methods. The values will be supplied in pixels and will be automatically converted to density independent values.

####fullscreenPlayFromUrl(jsonUrl, successCallback, errorCallback)

Calling this method will display a fullscreen overlay over the Cordova application, with the player centered inside.

The player configuration data is retrieved from the URL specified in the jsonUrl variable.

####fullscreenPlayFromObject(jsonObject, successCallback, errorCallback)

Calling this method will display a fullscreen overlay over the Cordova application, with the player centered inside.

The player configuration data is retrieved from the JSON object passed in the jsonObject variable.

####playFromUrl(jsonUrl, successCallback, errorCallback)

Calling this method will position the player inside the HTML element with the "player" ID and start playback using the configuration retrieved from jsonUrl.

All player status events are notified inside the successCallback(result) function.

In order to place the player directly on-screen, avoiding DOM elements, pass an object to the *playFromUrl* call:

	window.veeplay.playFromUrl({
		 "jsonUrl": "JSON URL",
		 "xPosition": 80,
		 "yPosition": 140,
		 "width": 200,
		 "height": 112
		}, successCallback, errorCallback);

####playFromObject(jsonObject, successCallback, errorCallback)

Calling this method will position the player inside the HTML element with the "player" ID and start playback using the configuration inside the jsonObject object.

All player status events are notified inside the successCallback(result) function.

In order to place the player directly on-screen, avoiding DOM elements, add a cordovaConfig property to the configuration object. Example:

	{
		"cordovaConfig": {
		  "xPosition": 70,
		  "yPosition": 180,
		  "width": 225,
		  "height": 125
		  },
		"content": [....]
	}

####stop(successCallback, errorCallback)

Calling this will stop the current playlist and reset the player.

####pause(successCallback, errorCallback)

Calling this will pause playback temporarily, if the player was playing.

####resume(successCallback, errorCallback)

Calling this method will resume playback if it was paused.

####getDuration(successCallback, errorCallback)

Calling this method will return the duration, in ms, of the currently playing video, inside the successCallback function. If the duration is not currently available, 0 will be returned.

####getBufferedTime(successCallback, errorCallback)

Calling this method will return the length of the buffer, in ms, inside the successCallback function. 

####toggleFullscreen(successCallback, errorCallback)

Calling this method will toggle full screen mode on and off.

####mute(successCallback, errorCallback)

This method will mute audio, if playback is running.

####unMute(successCallback, errorCallback)

This method will unmute audio, if playback is running.

####isPlaying(successCallback, errorCallback)

This method will call the successCallback method with "true" or "false", depending if the Veeplay player is currently rendering a video unit.

####isSeeking(successCallback, errorCallback)

This method will call the successCallback method with "true" or "false", depending if the Veeplay player is currently processing a seek operation.

####skip(successCallback, errorCallback)

This method will stop the playback of the current unit and start playback of the next unit in the playlist.

####back(successCallback, errorCallback)

This method will stop the playback of the current unit and start playback of the previous unit in the playlist.

###Intercepting player events

Veeplay emits a wide array of events that notify playback states, errors and user interactions. In order to intercept those events (for analytics tracking or for updating the UI, for example), use the following snippet:

        window.veeplay.onTrackingEvent = function(result) {
            console.log(JSON.stringify(result));
        }

The "result" variable is a Javascript object, with the following structure:

* `type` - the event being fired
* `error` - if the event represents an error, this should contain details
* `seek_start` - if the event represents a seek, this should indicate when the seek has started
* `playback_time` - the playback time when the event fired

###Google cast usage

Google Cast device streaming is achieved via bridging the Veeplay Cast plugin through Cordova. Within the player container, a MediaRoute button is automatically injected whenever a Google Cast device is present.

Due to the fact that the MediaRouter is dependent on the Google support library and the AppCompat versions of the Activity class, the setup is a little more complicated:

####Project setup

**A.** Add the following lines to the CordovaLib gradle file (located at *platforms/android/CordovaLib/build.gradle* in the root of your Cordova app):

	dependencies {
	    compile 'com.android.support:support-v4:23.1.1'
	}

**B.** Import the FragmentActivity class into the CordovaActivity class. Just add the following statement in the same block of code with the already existing import statements (the file is located at *platforms/android/CordovaLib/src/org/apache/cordova/CordovaActivity.javadiff*)

	import android.support.v4.app.FragmentActivity;

**C.** Change the declaration of the CordovaActivity class like below (just replace Activity with FragmentActivity)

	public class CordovaActivity extends FragmentActivity 

**D.** In the *platforms/android/AndroidManifest.xml* file, in the first **\<activity>** tag, replace

	android:theme="@android:style/Theme.DeviceDefault.NoActionBar"
  with 

	android:theme="@style/Theme.AppCompat"

####Integration instructions

Within the deviceready callback (or wherever convenient, early in the application initialization flow), add the following method calls:

        window.veeplay.appStarted();  //needed in order to place the player back on screen if the application got killed

        window.veeplay.configureCastSettings({
            "playText": "PlayButtonText",
            "pauseText": "PauseButtonText",
            "disconnectText": "DisconnectButtonText",
            "appName": "YourApplicationName",
            "appId": "E8CDF951"
        });

###Uninstall

    cordova plugin remove veeplay-cordova-plugin

###Licensing

The Veeplay Cordova plugin is available as an open-source component, under the Apache 2.0 license. Usage of the Veeplay SDK however is dependant on having a valid Veeplay license (you can sign up for a trial license at https://panel.veeplay.com).
