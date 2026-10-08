package me.shadow.eclipselauncher.pojav.lifecycle;

import static me.shadow.eclipselauncher.pojav.MainActivity.INTENT_VERSION;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import me.shadow.eclipselauncher.R;
import me.shadow.eclipselauncher.context.ContextExecutor;
import me.shadow.eclipselauncher.feature.version.Version;
import me.shadow.eclipselauncher.setting.AllSettings;

import me.shadow.eclipselauncher.pojav.MainActivity;
import me.shadow.eclipselauncher.pojav.Tools;
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper;
import me.shadow.eclipselauncher.pojav.tasks.AsyncMinecraftDownloader;
import me.shadow.eclipselauncher.pojav.utils.NotificationUtils;

public class ContextAwareDoneListener implements AsyncMinecraftDownloader.DoneListener, ContextExecutorTask {
    private final String mErrorString;
    private final Version mVersion;

    public ContextAwareDoneListener(Context baseContext, Version version) {
        this.mErrorString = baseContext.getString(R.string.mc_download_failed);
        this.mVersion = version;
    }

    private Intent createGameStartIntent(Context context) {
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra(INTENT_VERSION, mVersion);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return mainIntent;
    }

    private void executeTask() {
        ProgressKeeper.waitUntilDone(() -> ContextExecutor.executeTask(this));
    }

    @Override
    public void onDownloadDone() {
        executeTask();
    }

    @Override
    public void onDownloadFailed(Throwable throwable) {
        Tools.showErrorRemote(mErrorString, throwable);
    }

    @Override
    public void executeWithActivity(Activity activity) {
        try {
            Intent gameStartIntent = createGameStartIntent(activity);
            activity.startActivity(gameStartIntent);
            if (AllSettings.getQuitLauncher().getValue()) {
                activity.finish();
                android.os.Process.killProcess(android.os.Process.myPid()); //You should kill yourself, NOW!
            }
        } catch (Throwable e) {
            Tools.showError(activity.getBaseContext(), e);
        }
    }

    @Override
    public void executeWithApplication(Context context) {
        Intent gameStartIntent = createGameStartIntent(context);
        // Since the game is a separate process anyway, it does not matter if it gets invoked
        // from somewhere other than the launcher activity.
        // The only problem may arise if the launcher starts doing something when the user starts the notification.
        // So, the notification is automatically removed once there are tasks ongoing in the ProgressKeeper
        NotificationUtils.sendBasicNotification(context,
                R.string.notif_download_finished,
                R.string.notif_download_finished_desc,
                gameStartIntent,
                NotificationUtils.PENDINGINTENT_CODE_GAME_START,
                NotificationUtils.NOTIFICATION_ID_GAME_START
        );
        // You should keep yourself safe, NOW!
        // otherwise android does weird things...
    }
}
