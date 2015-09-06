package com.mvgv70.xposed_yandex_maps;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

public class Main implements IXposedHookLoadPackage {
	
  private static ImageButton findmeButton = null;
  private final static String TAG = "xposed-yandex-maps";
	
  @SuppressLint("HandlerLeak")
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    
    final Handler handler = new Handler() {
    	
      @Override
      public void handleMessage(Message msg) {
        if (findmeButton == null) return;
        // может и не нужно проверять видимость
  	    if (findmeButton.getVisibility() == View.VISIBLE)
    	{
          findmeButton.performClick();
          Log.d(TAG,"findmeButton pressed");
          if (msg.what > 0)
          {
            // нажмем на FindMeButton через 1 сек
        	int what = msg.what-1;
            sendEmptyMessageDelayed(what, 1000);
          }
    	}
      }
    };
    
    final LocationListener locationListener = new LocationListener() {
    	
      public void onLocationChanged(Location location)
      {
        Log.d(TAG,"GPS postion detected");
        // нажмем на FindMeButton через 1 сек
        handler.sendEmptyMessageDelayed(1, 1000);
      }
      
      public void onProviderDisabled(String provider) {}
      
      public void onProviderEnabled(String provider) {}
      
      public void onStatusChanged(String provider, int status, Bundle extras) {}
    };
    
    // FindMeButton(Context, AttributeSet)
    XC_MethodHook createFindMeButton = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"FindMeButton found");
        findmeButton = (ImageButton)param.thisObject;
      }
    }; 
    
    // MapActivity.onCreate(Bundle)
    XC_MethodHook createActivity = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"Activity.create");
        Activity activity = (Activity)param.thisObject;
        LocationManager locationManager = (LocationManager)activity.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) 
          // одноразовое определение координат
          locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        else
          Log.e(TAG,"locationManager not found!");
      }
    };
        
    // begin hooks
    if (!lpparam.packageName.equals("ru.yandex.yandexmaps")) return;
    XposedHelpers.findAndHookConstructor("ru.yandex.yandexmaps.gui.FindMeButton", lpparam.classLoader, Context.class, AttributeSet.class, createFindMeButton);
    XposedHelpers.findAndHookMethod("ru.yandex.yandexmaps.MapActivity", lpparam.classLoader, "onCreate", Bundle.class,  createActivity);
    Log.d(TAG,"ru.yandex.yandexmaps OK");
  }
  

}