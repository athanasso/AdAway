package org.adaway.util.log;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.topjohnwu.superuser.Shell;

import org.adaway.helper.PreferenceHelper;
import org.adaway.util.AppExecutors;

import timber.log.Timber;

/**
 * This class is an utility class that configures the application log.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public final class ApplicationLog {
    /**
     * Private constructor.
     */
    private ApplicationLog() {

    }

    /**
     * Initialize application logging.
     *
     * @param application The application instance.
     */
    public static void init(Application application) {
        if (isApplicationDebuggable(application)) {
            Shell.enableVerboseLogging = true;
            Timber.plant(new Timber.DebugTree());
            return;
        }
        Shell.enableVerboseLogging = false;
        SentryLog.init(application);
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (PreferenceHelper.getDebugEnabled(application)) {
                Shell.enableVerboseLogging = true;
                Timber.plant(new Timber.DebugTree());
            }
        });
    }

    private static boolean isApplicationDebuggable(Context context) {
        return (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }
}
