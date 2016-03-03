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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class Main implements IXposedHookLoadPackage {
	
  private static View findmeButton = null;
  private static OnClickListener listener = null;
  private static boolean headingMode = false;
  private final static String TAG = "xposed-yandex-maps";
  
  private Runnable pressButton = new Runnable()
  {
    public void run() 
    {
      Log.d(TAG,"Runnable: headingMode="+headingMode);
      // нажимаем на кнопку, еслирежим ориентации по движению не включен
      if (!headingMode)
      {
        findmeButton.performClick();
        listener.onClick(findmeButton);
      }
	}
  };
  
  private LocationListener locationListener = new LocationListener() 
  {
    public void onLocationChanged(Location location)
    {
      Log.d(TAG,"GPS postion detected");
      if (listener != null)
      {
    	Log.d(TAG,"GPS: headingMode="+headingMode);
    	// нажмем на кнопку 3 раза с интервалом 2 секунды
    	findmeButton.postDelayed(pressButton, 2000);
    	findmeButton.postDelayed(pressButton, 4000);
    	findmeButton.postDelayed(pressButton, 6000);
      }
      else
        Log.w(TAG,"FindMeButton not found");
    }
      
    public void onProviderDisabled(String provider) {}
      
    public void onProviderEnabled(String provider) {}
      
    public void onStatusChanged(String provider, int status, Bundle extras) {}
  };
	
  @Override
  public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
    
    // MapActivity.onCreate(Bundle)
    XC_MethodHook createActivity = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"Activity.create");
        headingMode = false;
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
    
    // FindMeButton.setHeadingMode(boolean)
    XC_MethodHook setHeadingMode = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        // включение/выключения режима ориентации карты по движению
        headingMode = (boolean)param.args[0]; 
        Log.d(TAG,"setHeadingMode: headingMode="+headingMode);
      }
    };
    
    // FindMeButton.onClickListener constructor
    XC_MethodHook findMeButtonListener = new XC_MethodHook() {
      
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG,"listener: "+param.thisObject.getClass().getName());
        if (param.thisObject instanceof View.OnClickListener)
        {
          findmeButton = (View)param.args[0];
          Log.d(TAG,"findmeButton="+findmeButton.getClass().getName());
          listener = (OnClickListener)param.thisObject;
        }
      }
    };
    
    // begin hooks
    if (!lpparam.packageName.equals("ru.yandex.yandexmaps")) return;
    XposedHelpers.findAndHookMethod("ru.yandex.yandexmaps.app.MapActivity", lpparam.classLoader, "onCreate", Bundle.class,  createActivity);
    // можно перехватить от 1 до 10, а в конструкторе проверить тип
    XposedHelpers.findAndHookConstructor("ru.yandex.maps.appkit.customview.FindMeButton$1", lpparam.classLoader, "ru.yandex.maps.appkit.customview.FindMeButton", findMeButtonListener);
    XposedHelpers.findAndHookMethod("ru.yandex.maps.appkit.customview.FindMeButton", lpparam.classLoader, "setHeadingMode", boolean.class,  setHeadingMode);
    // XposedHelpers.findAndHookMethod("ru.yandex.maps.appkit.customview.FindMeButton", lpparam.classLoader, "setInProgress", boolean.class,  setInProgress);
    Log.d(TAG,"ru.yandex.yandexmaps OK");
  }
  
}