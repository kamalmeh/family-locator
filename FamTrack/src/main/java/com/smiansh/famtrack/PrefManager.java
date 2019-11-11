package com.smiansh.famtrack;

import android.content.Context;
import android.content.SharedPreferences;

class PrefManager {
    static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    // Shared preferences file name
    private static final String PREF_NAME = "SAFE_CIRCLE_DEFAULT_PREFERENCE";
    private static final String IS_RATING_GIVEN = "isRatingGiven";
    private SharedPreferences applicationDefaultSharedPreferences;
    SharedPreferences.Editor editor;
    private Context _context;

    PrefManager(Context context) {
        setContext(context);
        setApplicationDefaultSharedPreferences();
        setEditor();
    }

    private void setEditor() {
        editor = getApplicationDefaultSharedPreferences().edit();
        editor.apply();
    }

    SharedPreferences.Editor getEditor() {
        return this.editor;
    }

    Context getContext() {
        return this._context;
    }

    private void setContext(Context context) {
        this._context = context;
    }

    SharedPreferences getApplicationDefaultSharedPreferences() {
        return this.applicationDefaultSharedPreferences;
    }

    private void setApplicationDefaultSharedPreferences() {
        this.applicationDefaultSharedPreferences = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    void enableFirstTimeLaunch() {
        editor.remove(IS_FIRST_TIME_LAUNCH).apply();
        editor.remove(IS_RATING_GIVEN).apply();
    }

    boolean isFirstTimeLaunch() {
        return this.applicationDefaultSharedPreferences.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    void disableFirstTimeLaunch() {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, false);
        editor.commit();
    }

}