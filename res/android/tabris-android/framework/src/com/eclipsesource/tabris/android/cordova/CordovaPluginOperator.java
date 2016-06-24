/**
 * ****************************************************************************
 * Copyright (c) Thieme Compliance GmbH
 * *****************************************************************************
 */

package com.eclipsesource.tabris.android.cordova;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;

import com.eclipsesource.tabris.android.TabrisActivity;
import com.eclipsesource.tabris.android.internal.toolkit.AppState;
import com.eclipsesource.tabris.android.internal.toolkit.IAppStateListener;
import com.eclipsesource.tabris.android.internal.toolkit.operator.AbstractWidgetOperator;
import com.eclipsesource.tabris.client.core.model.Properties;
import com.eclipsesource.tabris.client.core.operation.CallOperation;
import com.eclipsesource.tabris.client.core.operation.CreateOperation;
import com.eclipsesource.tabris.client.core.operation.DestroyOperation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.R;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import static com.eclipsesource.tabris.client.core.util.ParamCheck.notNull;

public class CordovaPluginOperator extends AbstractWidgetOperator {

  public static final String TYPE = "cordova.plugin";
  public static final String ACTION = "action";
  public static final String ARGUMENTS = "arguments";
  public static final String SERVICE = "service";

  private TabrisWebView webView;

  @Override
  public String getType() {
    return TYPE;
  }

  public CordovaPluginOperator( TabrisActivity activity ) {
    super( activity );
    webView = createWebView( activity );
    getWidgetToolkit().addAppStateListener( new CordovaAppStateListener() );
  }

  private TabrisWebView createWebView( TabrisActivity activity ) {
    TabrisWebView webView = new TabrisWebView( activity );
    CordovaInterfaceImpl cordovaInterface = new CordovaInterfaceImpl( activity, TabrisWebView.getExecutorService() );
    ConfigXmlParser configXmlParser = new ConfigXmlParser();
    configXmlParser.parse( getActivity() );
    webView.init( cordovaInterface, configXmlParser.getPluginEntries(), new CordovaPreferences() );
    ( ( ViewGroup )activity.findViewById( R.id.content_root ) ).addView( webView.getView(), 0 );
    webView.getView().setVisibility( View.GONE );
    return webView;
  }

  @Override
  public Object call( CallOperation operation ) {
    Properties properties = operation.getProperties();
    String action = properties.getString( ACTION );
    String callbackId = properties.getString( TabrisWebView.CALLBACK_ID );
    String arguments = properties.getString( ARGUMENTS );
    webView.mapCallbackIdToTarget( callbackId, operation.getTarget() );
    String service = ( String )getObjectRegistry().getObject( operation.getTarget() );
    exec( service, action, callbackId, arguments );
    return null;
  }

  protected void exec( String service, String action, String callbackId, String args ) {
    webView.getPluginManager().exec( service, action, callbackId, args );
  }

  @Override
  public void create( CreateOperation operation ) {
    notNull( operation.getProperties().getString( SERVICE ), SERVICE );
    String service = operation.getProperties().getString( SERVICE );
    getObjectRegistry().register( operation.getTarget(), service, operation.getType() );
  }

  @Override
  public void destroy( DestroyOperation operation ) {
    destroyCordovaWebView();
  }

  public void destroyCordovaWebView() {
    if( webView != null ) {
      webView.handleDestroy();
      webView = null;
    }
  }

  private class CordovaAppStateListener implements IAppStateListener {

    @Override
    public void stateChanged( AppState state, Intent intent ) {
      if( state == AppState.NEW_INTENT ) {
        webView.onNewIntent( intent );
      } else if( state == AppState.START ) {
        webView.handleStart();
      } else if( state == AppState.RESUME ) {
        webView.handleResume( true );
      } else if( state == AppState.PAUSE ) {
        webView.handlePause( true );
      } else if( state == AppState.STOP ) {
        webView.handleStop();
      } else if( state == AppState.CONFIGURATION ) {
        webView.getPluginManager().onConfigurationChanged( null );
      } else if( state == AppState.DESTROY ) {
        destroyCordovaWebView();
      }
    }
  }

}