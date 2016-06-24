package com.eclipsesource.tabris.android.cordova;

import android.annotation.SuppressLint;

import com.eclipsesource.tabris.android.TabrisActivity;
import com.eclipsesource.tabris.android.internal.javascript.JavaScriptMessageSender;
import com.eclipsesource.tabris.client.core.IMessageSender;
import com.eclipsesource.tabris.client.core.RemoteObject;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.NativeToJsMessageQueue;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.PluginResult;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint( "ViewConstructor" )
public class TabrisWebView extends CordovaWebViewImpl {

  public static final String CALLBACK_ID = "callbackId";
  public static final String STATUS = "status";
  public static final String MESSAGE = "message";
  public static final String FINISH = "finish";
  public static final String KEEP_CALLBACK = "keepCallback";

  private static ExecutorService executorService = Executors.newFixedThreadPool( 10 );

  private TabrisActivity activity;
  private Map<String, String> idToTargetMapping = new HashMap<>();
  private NativeToJsMessageQueue nativeToJsMessageQueue;

  public TabrisWebView( TabrisActivity activity ) {
    super( new SystemWebViewEngine( new SystemWebView( activity ) ) );
    this.activity = activity;
  }

  public static ExecutorService getExecutorService() {
    return executorService;
  }

  @Override
  public void init( CordovaInterface cordova, List<PluginEntry> pluginEntries, CordovaPreferences preferences ) {
    nativeToJsMessageQueue = new NativeToJsMessageQueue();
    nativeToJsMessageQueue.addBridgeMode( new NativeToJsMessageQueue.NoOpBridgeMode() );
    nativeToJsMessageQueue.setBridgeMode( 0 );
    super.init( cordova, pluginEntries, preferences );
  }

  @Override
  public void sendPluginResult( PluginResult result, String callbackId ) {
    String encodedResult = encodeResult( result, callbackId );
    if( encodedResult == null ) {
      return;
    }
    boolean keepCallback = result.getKeepCallback();
    int status = result.getStatus();
    Map<String, Object> event = createEvent( callbackId, keepCallback, encodedResult, status );
    RemoteObject remoteObject = activity.getRemoteObject( idToTargetMapping.get( callbackId ) );
    if( !keepCallback ) {
      idToTargetMapping.remove( callbackId );
    }
    if( remoteObject != null ) {
      sendResultOnUIThread( remoteObject, event );
    }
  }

  private String encodeResult( PluginResult result, String callbackId ) {
    nativeToJsMessageQueue.addPluginResult( result, callbackId );
    return nativeToJsMessageQueue.popAndEncode( false );
  }

  private Map<String, Object> createEvent( String callbackId, boolean keepCallback,
                                           String encodedResult, int status ) {
    Map<String, Object> event = new HashMap<>();
    event.put( CALLBACK_ID, callbackId );
    event.put( STATUS, status );
    event.put( KEEP_CALLBACK, keepCallback );
    event.put( MESSAGE, encodedResult );
    return event;
  }

  protected void sendResultOnUIThread( final RemoteObject remoteObject, final Map<String, Object> event ) {
    activity.runOnUiThread( new Runnable() {
      @Override
      public void run() {
        remoteObject.notify( FINISH, event );
      }
    } );
  }

  public void mapCallbackIdToTarget( String callbackId, String target ) {
    idToTargetMapping.put( callbackId, target );
  }

  @Override
  public void loadUrl( String url ) {
    IMessageSender messageSender = activity.getMessageSender();
    if( messageSender instanceof JavaScriptMessageSender ) {
      JavaScriptMessageSender sender = ( JavaScriptMessageSender )messageSender;
      if( url != null ) {
        String trimmedUrl = url.trim();
        if( trimmedUrl.startsWith( "javascript:" ) ) {
          String script = trimmedUrl.replaceFirst( "javascript:", "" );
          sender.evaluateScript( script );
        }
      }
    }
  }
}