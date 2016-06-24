/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package __ID__;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.eclipsesource.tabris.android.TabrisActivity;
import com.eclipsesource.tabris.android.internal.javascript.JavaScriptApp;
import com.eclipsesource.tabris.js.launcher.utils.DownloadService;
import com.eclipsesource.tabris.js.launcher.utils.LauncherApp;
import com.eclipsesource.tabris.js.launcher.utils.LoadRequest;
import com.eclipsesource.tabris.js.launcher.utils.LoadingIndicator;
import com.eclipsesource.tabris.js.launcher.utils.LocalResourcesLoader;
import com.eclipsesource.tabris.js.launcher.utils.PackageJson;
import com.eclipsesource.tabris.js.launcher.utils.RemoteLauncher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.eclipsesource.tabris.android.TabrisActivity.RESULT_EXTRA_URL;
import static com.eclipsesource.tabris.js.launcher.utils.Preferences.*;

import org.apache.cordova.*;

public class __ACTIVITY__ extends CordovaActivity implements RemoteLauncher {

  public static final String URI_ANDROID_ASSET = "file:///android_asset/";

  private SharedPreferences prefs;
  private LoadingIndicator loadingIndicator;

  @Override
  public void onCreate( Bundle savedInstanceState ) {
    super.onCreate( savedInstanceState );
    super.init();
    setContentView( com.eclipsesource.tabris.js.launcher.utils.R.layout.main );
    loadingIndicator = new LoadingIndicator( this, true );
    prefs = PreferenceManager.getDefaultSharedPreferences( this );
    String localResourceJsonUri = getLocalResourceJsonUri();
    if( localResourceJsonUri != null ) {
      startTabrisFromLocalResources( localResourceJsonUri );
    } else {
      if( savedInstanceState == null ) {
        if( prefs.getBoolean( PREF_AUTO_START, PREF_AUTO_START_DEFAULT ) ) {
          startTabrisFromRecentlyLaunched();
        }
      }
    }
  }

  private String getLocalResourceJsonUri() {
    String resourceJsonFromCordovaConfig = getLocalResourceJsonFromCordovaConfig();
    if( resourceJsonFromCordovaConfig != null ) {
      return resourceJsonFromCordovaConfig;
    }
    String resourceJsonFromBuildConfig = getLocalResourceJsonFromBuildConfig();
    if( resourceJsonFromBuildConfig != null ) {
      return resourceJsonFromBuildConfig;
    }
    return null;
  }

  private String getLocalResourceJsonFromCordovaConfig() {
    if( launchUrl != null && launchUrl.endsWith( PackageJson.PACKAGE_JSON ) ) {
      return launchUrl.replaceFirst( "file:///android_asset/", "" );
    }
    return null;
  }

  private String getLocalResourceJsonFromBuildConfig() {
    try {
      getAssets().open( BuildConfig.JS_APP ).close();
      return BuildConfig.JS_APP;
    } catch( IOException e ) {
      return null;
    }
  }

  private void startTabrisFromLocalResources( String localResourceJsonUri ) {
    try {
      LocalResourcesLoader loader = new LocalResourcesLoader( getAssets(), localResourceJsonUri );
      JavaScriptApp javaScriptApp = new JavaScriptApp( loader.getBaseUri() );
      javaScriptApp.addFiles( loader.load() );
      javaScriptApp.setLocalResourceRootUris( getLocalResourceRootUris() );
      startTabrisActivity( javaScriptApp, localResourceJsonUri.equals( getLocalResourceJsonUri() ) );
    } catch( FileNotFoundException e ) {
      showError( "Could not find file:\n\n" + e.getMessage() );
    } catch( Exception e ) {
      showError( "Can not start app.\n\n" + e.getMessage() );
    }
  }

  private List<Uri> getLocalResourceRootUris() {
    List<String> uriStrings = Splitter.on( ',' ).splitToList( BuildConfig.LOCAL_RESOURCE_ROOT_URIS );
    if( !uriStrings.isEmpty() ) {
      List<Uri> uris = new ArrayList<>();
      for( String string : uriStrings ) {
        uris.add( Uri.parse( string ) );
      }
      return uris;
    }
    return null;
  }

  private void startTabrisActivity( JavaScriptApp javaScriptApp, boolean standaloneApp ) {
    Intent intent = new Intent( this, TabrisActivity.class );
    intent.putExtra( JavaScriptApp.EXTRA_JAVA_SCRIPT_APP, javaScriptApp );
    intent.putExtra( TabrisActivity.EXTRA_DEV_MODE, preferences.getBoolean( "EnableDeveloperConsole", false) );
    intent.putExtra( TabrisActivity.EXTRA_STRICT_SSL, preferences.getBoolean( "UseStrictSSL", true) );
    intent.putExtra( TabrisActivity.EXTRA_THEME, preferences.getString( "Theme", null) );
    intent.putExtra( TabrisActivity.EXTRA_SHOW_SYSTEM_BAR, !preferences.getBoolean( "Fullscreen", false ) );
    intent.putExtra( TabrisActivity.EXTRA_STANDALONE_APP, standaloneApp );
    startActivityForResult( intent, 0 );
  }

  @Override
  protected void onResume() {
    super.onResume();
    loadingIndicator.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    loadingIndicator.onPause();
  }

  @Override
  protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
    if( resultCode == TabrisActivity.RESULT_CODE_RESTART ) {
      handleResultRestart( data );
    } else if( resultCode == TabrisActivity.RESULT_CODE_ERROR ) {
      handleResultError( data );
    } else if( resultCode == TabrisActivity.RESULT_CODE_CLOSE ) {
      finish();
    }
  }

  private void handleResultRestart( Intent data ) {
    String localResourceJsonUri = getLocalResourceJsonUri();
    if( data != null && data.hasExtra( TabrisActivity.RESULT_EXTRA_URL ) ) {
      startTabrisFromUrl( data.getStringExtra( TabrisActivity.RESULT_EXTRA_URL ) );
    } else if( localResourceJsonUri != null ) {
      startTabrisFromLocalResources( localResourceJsonUri );
    } else {
      startTabrisFromRecentlyLaunched();
    }
  }

  private void startTabrisFromUrl( String url ) {
    if( Uri.parse( url ).isRelative() ) {
      startTabrisFromLocalResources( getAppBasePath() + url );
    } else if( url.startsWith( URI_ANDROID_ASSET ) ) {
      String localResourceJsonUri = url.replaceFirst( URI_ANDROID_ASSET, "" ) + "/" + PackageJson.PACKAGE_JSON;
      startTabrisFromLocalResources( localResourceJsonUri );
    } else {
      startTabrisFromRemoteResources( new LoadRequest( url ) );
    }
  }

  private String getAppBasePath() {
    String localResourceJsonUri = getLocalResourceJsonUri();
    List<String> pathSegments = new ArrayList<>( Uri.parse( localResourceJsonUri ).getPathSegments() );
    if( !pathSegments.isEmpty() ) {
      pathSegments.remove( pathSegments.size() - 1 );
      return Joiner.on( '/' ).join( pathSegments ) + "/";
    }
    return "";
  }

  private void startTabrisFromRecentlyLaunched() {
    String loadRequestJson = prefs.getString( PREF_LAST_LAUNCHED_LOAD_REQUEST, null );
    if( loadRequestJson != null ) {
      LoadRequest loadRequest = LauncherApp.getGson().fromJson( loadRequestJson, LoadRequest.class );
      startTabrisFromRemoteResources( loadRequest );
    }
  }

  private void handleResultError( Intent data ) {
    if( data != null ) {
      showError( data.getStringExtra( TabrisActivity.RESULT_MESSAGE ) );
    } else {
      showError( "The app did not work as expected." );
    }
  }

  private void showError( String message ) {
    AlertDialog.Builder builder = new AlertDialog.Builder( this );
    builder.setTitle( R.string.app_name );
    builder.setMessage( message );
    builder.setNeutralButton( android.R.string.ok, null );
    builder.show();
  }

  public void startTabrisFromRemoteResources( LoadRequest loadRequest ) {
    loadingIndicator.scheduleShowLoading( loadRequest.getUrl() );
    loadRequest.setUseRequestCache( prefs.getBoolean( PREF_HTTP_CACHE, PREF_HTTP_CACHE_DEFAULT ) );
    loadRequest.setLocalResourceRootUris( getLocalResourceRootUris() );
    String loadRequestJson = LauncherApp.getGson().toJson( loadRequest );
    prefs.edit().putString( PREF_LAST_LAUNCHED_LOAD_REQUEST, loadRequestJson ).apply();
    Intent serviceIntent = new Intent( this, DownloadService.class );
    serviceIntent.putExtra( DownloadService.EXTRA_LOAD_REQUEST, loadRequest );
    startService( serviceIntent );
  }

}
