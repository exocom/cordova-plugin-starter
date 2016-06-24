var exec = require('cordova/exec');
window._watchDogTimer = null;

function Veeplay() {
    var self = this;

    self.playerId = "player";
    self.pollInterval = 20;

    function getDivRect(div) {
        if (!div) {
            return;
        }
        var pageRect = getPageRect();
        var rect = div.getBoundingClientRect();
        var divRect = {
            'left': rect.left + pageRect.left,
            'top': rect.top + pageRect.top,
            'width': rect.width,
            'height': rect.height
        };
        return divRect;
    }

    function getPageRect() {
        var doc = document.documentElement;
        var pageWidth = window.innerWidth ||
            document.documentElement.clientWidth ||
            document.body.clientWidth,
            pageHeight = window.innerHeight ||
            document.documentElement.clientHeight ||
            document.body.clientHeight;
        var pageLeft = (window.pageXOffset || doc.scrollLeft) - (doc.clientLeft || 0);
        var pageTop = (window.pageYOffset || doc.scrollTop) - (doc.clientTop || 0);

        return {
            'width': pageWidth,
            'height': pageHeight,
            'left': pageLeft,
            'top': pageTop
        };
    }

    self.startMonitoring = function() {
        var prevSize = null;
        var divSize;
            var div = null; // document.getElementById(window.veeplay.playerId);

        if (window._watchDogTimer) {
            clearInterval(window._watchDogTimer);
        }

        function init() {
            window._watchDogTimer = window.setInterval(function() {
                monitor();
            }, self.pollInterval);
        }

        function monitor() {
            if (div) {
                divSize = getDivRect(div);
                if (prevSize) {
                    if (divSize.left != prevSize.left ||
                        divSize.top != prevSize.top ||
                        divSize.width != prevSize.width ||
                        divSize.height != prevSize.height) {
                        self.setBounds(divSize);
                    }
                }
                prevSize = divSize;
            }
            divSize = null;
            clearInterval(window._watchDogTimer);
            init();
        }

        init();
    }

    self.stopMonitoring = function() {
        if (window._watchDogTimer) {
            clearInterval(window._watchDogTimer);
        }
        window._watchDogTimer = null;
    }

    self.setBounds = function(rect) {
        rect = rect || getDivRect(document.getElementById(window.veeplay.playerId));
        exec(null, null, "veeplay-cordova-plugin", "setBounds", [rect.left, rect.top, rect.width, rect.height]);
    }

    self.appStarted = function() {
        exec(null, null, "veeplay-cordova-plugin", "appStarted", []);
    };

    self.configureCastSettings = function(castConfiguration, success, error) {
        var playText;
        if(castConfiguration.hasOwnProperty('playText')) {
            playText = castConfiguration.playText;
        }
        var pauseText;
        if(castConfiguration.hasOwnProperty('pauseText')) {
            pauseText = castConfiguration.pauseText;
        }
        var disconnectText;
        if(castConfiguration.hasOwnProperty('disconnectText')) {
            disconnectText = castConfiguration.disconnectText;
        }
        var appName;
        if(castConfiguration.hasOwnProperty('appName')) {
            appName = castConfiguration.appName;
        }
        var appId;
        if(castConfiguration.hasOwnProperty('appId')) {
            appId = castConfiguration.appId;
        }
        exec(null, null, "veeplay-cordova-plugin", "configureCastSettings", [playText, pauseText, disconnectText, appName, appId]);
    }

    self.playFromUrl = function(arg0, success, error) {
        if(typeof arg0 === 'string') {
            self.play(arg0, success, error);
        } else {
            if(arg0.hasOwnProperty('jsonUrl')) {
                self.play(arg0, success, error);
            }
        }
    }

    self.playFromObject = function(arg0, success, error) {
        self.play(arg0, success, error);
    }

    self.fullscreenPlayFromUrl = function(arg0, success, error) {
        var obj = {
            "jsonUrl": arg0,
            "fullscreen": true
        }
        self.play(obj, success, error);
    }

    self.fullscreenPlayFromObject = function(arg0, success, error) {
        if(!arg0.cordovaConfig) {
            arg0.cordovaConfig = {}
        }
        arg0.cordovaConfig.fullscreen = true;
        self.play(arg0, success, error);
    }

    self.play = function(arg0, success, error) {
        var div = null; //document.getElementById(window.veeplay.playerId);
        var rect;
        var jsonUrl = null;
        var jsonObject = null;
        var fullscreen = false;
        self.stopMonitoring();
        if(typeof arg0 === 'string') {
            if (!div) {
                error("No element with id " + window.veeplay.playerId);
                return;
            }
            self.startMonitoring();
            rect = getDivRect(div);
            jsonUrl = arg0;

        } else {
            var cordovaConfig = arg0.cordovaConfig || arg0;
            if(cordovaConfig.hasOwnProperty('jsonUrl')) {
                jsonUrl = cordovaConfig.jsonUrl;
            } else {
                jsonObject = JSON.stringify(arg0);
            }
            
            if( !cordovaConfig.hasOwnProperty('xPosition') ||
                !cordovaConfig.hasOwnProperty('yPosition') ||
                !cordovaConfig.hasOwnProperty('width') ||
                !cordovaConfig.hasOwnProperty('height')) {
                    if (!div && !cordovaConfig.fullscreen) {
                        error("No element with id " + window.veeplay.playerId);
                        return;
                    } else if (!cordovaConfig.fullscreen) {
                        self.startMonitoring();
                        rect = getDivRect(div);
                    }

            } else {
                rect = {
                    'top': cordovaConfig.yPosition,
                    'width': cordovaConfig.width,
                    'height': cordovaConfig.height,
                    'left': cordovaConfig.xPosition
                };
            }

            if (cordovaConfig.fullscreen) {
                fullscreen = true;
            }
        }
        exec(internalBridgeCall, function() {}, "veeplay-cordova-plugin", "bindInternalBridge", []);
        exec(self.onTrackingEvent, function() {}, "veeplay-cordova-plugin", "bindEventsBridge", []);
        exec(success, error, "veeplay-cordova-plugin", "play", [jsonUrl?jsonUrl:jsonObject, rect.left, rect.top, rect.width, rect.height, fullscreen]);
    }

    self.stop = function(success, error) {
        self.stopMonitoring();
        exec(success, error, "veeplay-cordova-plugin", "stop", []);
    };

    self.pause = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "pause", []);
    };

    self.resume = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "resume", []);
    };

    self.getDuration = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "duration", []);
    };

    self.getBufferedTime = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "bufferedTime", []);
    };

    self.toggleFullscreen = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "toggleFullscreen", []);
    };

    self.mute = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "mute", []);
    };

    self.unMute = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "unMute", []);
    };

    self.isPlaying = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "isPlaying", []);
    };

    self.isSeeking = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "isSeeking", []);
    };

    self.skip = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "skip", []);
    };

    self.back = function(success, error) {
        exec(success, error, "veeplay-cordova-plugin", "back", []);
    };

    self.onTrackingEvent = function() {
    }

    function internalBridgeCall(result) {
        if(result == "stopBoundingTimer") {
            self.stopMonitoring();
        }
    }
}

module.exports = new Veeplay();