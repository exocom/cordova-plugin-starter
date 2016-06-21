## PreSetup
Some plugins require that your app name matches your real app.


Rename the App id found config.xml under __widget id__. From com.blank.test To {{your app id}}
config.xml
```
<widget id="com.blank.test" ...
```

### For reference
Find the plugin you wish to work on and install it via cordova cli (Under WebStorm section)
```
$ cordova plugin add {{plugin name}}
```

If applicable find the plaform you wish to work on IE not standard such as tabris, tv, etc. (Under Android Studio section)
```
$ cordova platform add {{dirctory containing platform files}}
```

## WebStorm
### Plugins (File->Settings->Plugins)
* PhoneGap/Cordova
* Bash Support
* Markdown Navigator

### Run/Debug configurations
In order for the cordova Run commands to work you will need to configure the path to the project. 
You will also need to plugin your android device and select it from the Edit configuration Section

#### PhoneGap/Cordova Working directory (File->Run->Edit Configurations)
Replace $FILL_THIS_OUT$ to this directory IE the directory that you are reading this readme from :)
In case your unsure about which directory to use, it is the one that contains (hooks/ plaforms/ plugins/ www/ config.xml)

#### Emulator and Phone (Run->Edit Configurations)
Plugin you device
Choose the matching profile IE PhoneGap/Cordova->Emulator (See RunDebug.png)
Under Specify target click refresh and choose device.


## Android Studio
### Pre Setup Commands
**Note many cli comands will remove the .idea folder from the $ProjectDir$/plaforms/android/.idea Just restore it from git.**

Example
```
$ cordova plugins install cordova-plugin-dialogs
$ cordova platform add android
```


### Open
* Restore the .idea folder (optional)
* open the $ProjectRoot$/platforms/android in Android Studio by following these instructions:
https://cordova.apache.org/docs/en/latest/guide/platforms/android/index.html#opening-a-project-in-android-studio

## Working Directory
It is best to work on your plugin in the plugins folder then use the platform.sh command to apply changes to the cordova project.

A dangerous but much quicker way is to work directly in the platforms/android for example in Android Studio then run cordova build.
All __WORK WILL BE LOST__  when __YOU__ forget to move all your changes back into the pluginis/ and run a cordova cli command. So use at your own discretion.



## How this project was created (Assuming the above has been completed)

Created cordova project
```
$ npm install cordova -g
$ cordova create blank com.blank.test TestApp
```

Opened the cordova project in WebStorm

Configured Configurations using drop down in upper right Edit Configurations

Added Phonegap/Cordova task for Android VM/Emulator
For both configured command:run plaform:android Specify Target:(Choose Device) Note: Device must be visible to adb IE turned on or connected

Created platform.sh and readme.md

Added platform for either android or other.
```
$ cordova platform add android
```


Opened platforms/android in Android Studio then configured

Added files to git including the .idea at platforms/android/.idea so that android studio could be easily restored