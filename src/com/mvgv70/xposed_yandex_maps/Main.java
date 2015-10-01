package com.mvgv70.xposed_yandex_maps;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;

public class Main implements IXposedHookLoadPackage {
	
  private static ImageButton findmeButton = null;
  private final static String TAG = "xposed-yandex-maps";
  
  private Runnable pressButton = new Runnable()
  {
    public void run() 
    {
      findmeButton.performClick();
      Log.d(TAG,"findmeButton pressed");
	}
  };
    
  private LocationListener locationListener = new LocationListener() 
  {
    public void onLocationChanged(Location location)
    {
      Log.d(TAG,"GPS postion detected");
      // нажмем на FindMeButton через 1 сек и через 2 сек
      findmeButton.postDelayed(pressButton, 1000);
      findmeButton.postDelayed(pressButton, 2000);
    }
      
    public void onProviderDisabled(String provider) {}
      
    public void onProviderEnabled(String provider) {}
      
    public void onStatusChanged(String provider, int status, Bundle extras) {}
  };
	
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    
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
        // показать версию модуля
        try 
        {
     	  Context context = activity.createPackageContext(getClass().getPackage().getName(), Context.CONTEXT_IGNORE_SECURITY);
     	  String version = context.getString(R.string.app_version_name);
          Log.d(TAG,"version="+version);
     	} catch (NameNotFoundException e) {}
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