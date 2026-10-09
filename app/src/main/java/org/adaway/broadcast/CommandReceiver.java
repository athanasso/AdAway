package org.adaway.broadcast;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import org.adaway.AdAwayApplication;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.error.HostErrorException;
import org.adaway.util.AppExecutors;

import timber.log.Timber;

/**
 * This broadcast receiver listens to commands from broadcast.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class CommandReceiver extends BroadcastReceiver {
    /**
     * This action allows to send commands to the application. See {@link Command} for extra values.
     */
    public static final String SEND_COMMAND_ACTION = "org.adaway.action.SEND_COMMAND";
    private static final int REQUEST_CODE_RESUME_ALARM = 44;
    private static final AppExecutors EXECUTORS = AppExecutors.getInstance();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SEND_COMMAND_ACTION.equals(intent.getAction())) {
            AdBlockModel adBlockModel = ((AdAwayApplication) context.getApplicationContext()).getAdBlockModel();
            Command command = Command.readFromIntent(intent);
            Timber.i("CommandReceiver invoked with command %s.", command);
            EXECUTORS.diskIO().execute(() -> executeCommand(context, adBlockModel, command));
        }
    }

    private void executeCommand(Context context, AdBlockModel adBlockModel, Command command) {
        try {
            switch (command) {
                case START:
                    cancelSnoozeAlarm(context);
                    adBlockModel.apply();
                    break;
                case STOP:
                    cancelSnoozeAlarm(context);
                    adBlockModel.revert();
                    break;
                case PAUSE_5MIN:
                    adBlockModel.revert();
                    scheduleResumeAlarm(context, 5 * 60 * 1000L);
                    break;
                case UNKNOWN:
                    Timber.i("Failed to run an unsupported command.");
                    break;
            }
        } catch (HostErrorException e) {
            Timber.w(e, "Failed to apply ad block command " + command + ".");
        }
    }

    private void scheduleResumeAlarm(Context context, long delayMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent resumeIntent = new Intent(context, CommandReceiver.class)
                    .setAction(SEND_COMMAND_ACTION);
            Command.START.appendToIntent(resumeIntent);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_RESUME_ALARM,
                    resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            long triggerAt = SystemClock.elapsedRealtime() + delayMillis;
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent);
        }
    }

    private void cancelSnoozeAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent resumeIntent = new Intent(context, CommandReceiver.class)
                    .setAction(SEND_COMMAND_ACTION);
            Command.START.appendToIntent(resumeIntent);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_RESUME_ALARM,
                    resumeIntent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }
}
