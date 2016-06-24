package com.veeplay.cordova;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by andrei on 5/4/16.
 */
public class VeeplayCastConfigurationWrapper {
    private String playText;
    private String pauseText;
    private String disconnectText;
    private String appName;
    private String appId;

    public VeeplayCastConfigurationWrapper() {
        this(null, null, null, null, null);
    }

    public VeeplayCastConfigurationWrapper(String playText, String pauseText, String disconnectText, String appName, String appId) {
        if(playText!=null && !playText.equals("null"))
            this.playText = playText;
        else
            this.playText = "Play";

        if(pauseText!=null && !pauseText.equals("null"))
            this.pauseText = pauseText;
        else
            this.pauseText = "Pause";

        if(disconnectText!=null && !disconnectText.equals("null"))
            this.disconnectText = disconnectText;
        else
            this.disconnectText = "Stop";

        if(appName!=null && !appName.equals("null"))
            this.appName = appName;
        else
            this.appName = "Playing on Google Cast";

        if(appId!=null && !appId.equals("null"))
            this.appId = appId;
        else
            this.appId = "E8CDF951";
    }

    public VeeplayCastConfigurationWrapper(JSONArray configurationArguments) throws JSONException {
        this(configurationArguments.getString(0), configurationArguments.getString(1), configurationArguments.getString(2), configurationArguments.getString(3), configurationArguments.getString(4));
    }

    public String getPlayText() {
        return playText;
    }

    public String getPauseText() {
        return pauseText;
    }

    public String getDisconnectText() {
        return disconnectText;
    }

    public String getAppName() {
        return appName;
    }

    public String getAppId() {
        return appId;
    }
}
