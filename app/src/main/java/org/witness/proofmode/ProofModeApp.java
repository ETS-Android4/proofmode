package org.witness.proofmode;

import static org.witness.proofmode.ProofMode.PREFS_DOPROOF;

import android.content.Context;

import androidx.multidex.MultiDexApplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import org.witness.proofmode.util.SafetyNetCheck;

import timber.log.Timber;

/**
 * Created by n8fr8 on 10/10/16.
 */
public class ProofModeApp extends MultiDexApplication {


    public final static String TAG = "ProofMode";


    @Override
    public void onCreate() {
        super.onCreate();


            init(this);

    }

    public static void init (Context context)
    {

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (prefs.getBoolean(PREFS_DOPROOF,false)) {

            ProofMode.init(context);

            Intent intentService = new Intent(context, ProofService.class);

            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intentService);

            } else {
                context.startService(intentService);
            }

        }
    }

    public static void cancel (Context context)
    {
        ProofMode.stop(context);
        Intent intentService = new Intent(context, ProofService.class);
        context.stopService(intentService);

    }

    /** A tree which logs important information for crash reporting. */
    private static class CrashReportingTree extends Timber.Tree {
        @Override protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }
        }
    }

}
