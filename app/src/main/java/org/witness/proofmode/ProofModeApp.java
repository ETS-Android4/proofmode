package org.witness.proofmode;

import android.content.Context;

import androidx.multidex.MultiDexApplication;
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


        SafetyNetCheck.setApiKey(getString(R.string.verification_api_key));


        init(this);
    }

    public static void init (Context context)
    {

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }

        ProofMode.init(context);
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
