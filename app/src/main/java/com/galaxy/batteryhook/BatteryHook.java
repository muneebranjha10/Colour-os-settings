package com.galaxy.batteryhook;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.Set;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class BatteryHook implements IXposedHookLoadPackage {
    private static final String PKG = "com.samsung.android.lool";
    private static final String ACT = "com.samsung.android.sm.battery.ui.BatteryActivity";
    private static final String TAG = "[GalaxyBatteryStage1] ";
    private static final int MENU_ID = 0x6ab01234;
    private static final String MENU_LABEL = "Custom battery test";
    private final Set<Activity> seen = Collections.newSetFromMap(new WeakHashMap<Activity, Boolean>());
    private void log(String text) { try { XposedBridge.log(TAG + text); } catch (Throwable ignored) {} }

    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) {
        if (!PKG.equals(p.packageName) || !PKG.equals(p.processName)) return;
        try {
            XposedHelpers.findAndHookMethod(ACT, p.classLoader, "onResume", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Activity a = (Activity) param.thisObject;
                        if (seen.add(a)) {
                            log("BatteryActivity onResume reached");
                            Toast.makeText(a, "Custom battery hook active", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Throwable e) { log("resume callback: " + e); }
                }
            });
            log("onResume hook installed");
        } catch (Throwable e) { log("onResume hook unavailable: " + e); }
        try {
            XposedHelpers.findAndHookMethod(ACT, p.classLoader, "onOptionsItemSelected", MenuItem.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        MenuItem item = (MenuItem) param.args[0];
                        if (item == null || item.getItemId() != MENU_ID || !MENU_LABEL.contentEquals(item.getTitle())) return;
                        showBattery((Activity)param.thisObject);
                        param.setResult(true);
                    } catch (Throwable e) { log("menu callback: " + e); }
                }
            });
            XposedHelpers.findAndHookMethod(ACT, p.classLoader, "onCreateOptionsMenu", Menu.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Menu menu = (Menu)param.args[0];
                        if (menu == null || menu.findItem(MENU_ID) != null) return;
                        menu.add(Menu.NONE, MENU_ID, Menu.NONE, MENU_LABEL).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                        param.setResult(true);
                        log("test menu injected");
                    } catch (Throwable e) { log("menu injection: " + e); }
                }
            });
            log("menu hooks installed");
        } catch (Throwable e) { log("menu hook unavailable: " + e); }
    }

    private void showBattery(Activity a) {
        Intent i = a.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        String levelText = "Unavailable";
        String statusText = "Unknown";
        if (i != null) {
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0 && level <= scale) levelText = Math.round(100f * level / scale) + "%";
            int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING: statusText = "Charging"; break;
                case BatteryManager.BATTERY_STATUS_FULL: statusText = "Full"; break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING: statusText = "Discharging"; break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING: statusText = "Not charging"; break;
            }
        }
        new AlertDialog.Builder(a).setTitle("Custom battery test")
            .setMessage("Battery level: " + levelText + "\nStatus: " + statusText + "\n\nStage 1 integration test. No settings changed.")
            .setPositiveButton(android.R.string.ok, null).show();
        log("battery dialog shown");
    }
}
