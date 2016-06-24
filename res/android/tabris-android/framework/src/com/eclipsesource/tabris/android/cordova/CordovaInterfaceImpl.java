package com.eclipsesource.tabris.android.cordova;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.eclipsesource.tabris.android.TabrisActivity;
import com.eclipsesource.tabris.android.internal.toolkit.IActivityResultListener;
import com.eclipsesource.tabris.android.internal.toolkit.IAndroidWidgetToolkit;
import com.eclipsesource.tabris.android.internal.toolkit.IRequestPermissionResultListener;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.json.JSONException;

import java.util.concurrent.ExecutorService;

public class CordovaInterfaceImpl implements CordovaInterface {

  private TabrisActivity activity;
  private ExecutorService executorService;
  private CordovaPlugin permissionResultCallback;

  public CordovaInterfaceImpl( TabrisActivity activity, ExecutorService executorService ) {
    this.activity = activity;
    this.executorService = executorService;
    final IAndroidWidgetToolkit widgetToolkit = activity.getWidgetToolkit();
    widgetToolkit.addRequestPermissionResult( new IRequestPermissionResultListener() {
      @Override
      public void permissionsResultReceived(int requestCode, String[] permissions, int[] grantResults ) {
        try {
          onRequestPermissionResult( requestCode, permissions, grantResults );
        } catch( Exception e ) {
          widgetToolkit.showError( e );
        }
      }
    } );
  }

  @Override
  public void startActivityForResult( final CordovaPlugin cordovaPlugin, Intent intent, final int originalRequestCode ) {
    final IAndroidWidgetToolkit widgetToolkit = activity.getWidgetToolkit();
    int requestCode = widgetToolkit.getRequestCodePool().takeRequestCode();
    widgetToolkit.addActivityResultListener( new IActivityResultListener() {
      @Override
      public void receivedActivityResult( int requestCode, int resultCode, Intent intent ) {
        widgetToolkit.removeActivityResultListener( this );
        widgetToolkit.getRequestCodePool().returnRequestCode( requestCode );
        cordovaPlugin.onActivityResult( originalRequestCode, resultCode, intent );
      }
    } );
    activity.startActivityForResult( intent, requestCode );
  }

  @Override
  public void setActivityResultCallback( CordovaPlugin plugin ) {
    // We use the tabris IActivityResultListener to return the result to the plugin.
    // See CordovaInterfaceImpl#startActivityForResult
  }

  @Override
  public Activity getActivity() {
    return activity;
  }

  @Override
  public Object onMessage( String id, Object data ) {
    LOG.d( "Tabris.js/Cordova", "unsupported onMessage(" + id + "," + data + ")" );
    return null;
  }

  @Override
  public ExecutorService getThreadPool() {
    return executorService;
  }

  /**
   * Called by the system when the user grants permissions
   *
   * @param requestCode
   * @param permissions
   * @param grantResults
   */
  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                        int[] grantResults) throws JSONException {
    if(permissionResultCallback != null)
    {
      permissionResultCallback.onRequestPermissionResult(requestCode, permissions, grantResults);
      permissionResultCallback = null;
    }
  }

  public void requestPermission(CordovaPlugin plugin, int requestCode, String permission) {
    permissionResultCallback = plugin;
    String[] permissions = new String [1];
    permissions[0] = permission;
    getActivity().requestPermissions(permissions, requestCode);
  }

  public void requestPermissions(CordovaPlugin plugin, int requestCode, String [] permissions)
  {
    permissionResultCallback = plugin;
    getActivity().requestPermissions(permissions, requestCode);
  }

  public boolean hasPermission(String permission)
  {
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    {
      int result = activity.checkSelfPermission(permission);
      return PackageManager.PERMISSION_GRANTED == result;
    }
    else
    {
      return true;
    }
  }

}
