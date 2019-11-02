package com.smiansh.famtrack;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    // Shared preferences file name
    public static final String PREF_NAME = "safecircle-welcome";
    public static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    public static final String IS_RATING_GIVEN = "isRatingGiven";
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context _context;
    // shared pref mode
    int PRIVATE_MODE = 0;

    public PrefManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
        editor.apply();
    }

    public void resetFirstTimeLaunch() {
        editor.remove(IS_FIRST_TIME_LAUNCH).apply();
        editor.remove(IS_RATING_GIVEN).apply();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

}