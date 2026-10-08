package me.shadow.eclipselauncher.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import me.shadow.eclipselauncher.context.ContextExecutor;
import me.shadow.eclipselauncher.context.LocaleHelper;
import me.shadow.eclipselauncher.event.single.LauncherIgnoreNotchEvent;
import me.shadow.eclipselauncher.feature.accounts.AccountsManager;
import me.shadow.eclipselauncher.feature.customprofilepath.ProfilePathManager;
import me.shadow.eclipselauncher.plugins.PluginLoader;
import me.shadow.eclipselauncher.renderer.Renderers;
import me.shadow.eclipselauncher.setting.AllSettings;
import me.shadow.eclipselauncher.utils.StoragePermissionsUtils;

import me.shadow.eclipselauncher.pojav.MissingStorageActivity;
import me.shadow.eclipselauncher.pojav.Tools;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.Companion.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleHelper.Companion.setLocale(this);
        Tools.setFullscreen(this);
        Tools.updateWindowSize(this);

        checkStoragePermissions();
        // Load the renderer
        Renderers.INSTANCE.init(false);
        // Load the plugins
        PluginLoader.loadAllPlugins(this, false);
        // Refresh the game paths
        ProfilePathManager.INSTANCE.refreshPath();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        if (!Tools.checkStorageRoot()) {
            startActivity(new Intent(this, MissingStorageActivity.class));
            finish();
        }

        checkStoragePermissions();

        AccountsManager.INSTANCE.reload();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.setFullscreen(this);
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Tools.getDisplayMetrics(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void event(LauncherIgnoreNotchEvent event) {
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    /** @return Whether or not the notch should be ignored */
    public boolean shouldIgnoreNotch() {
        return AllSettings.getIgnoreNotchLauncher().getValue();
    }

    private void checkStoragePermissions() {
        // Check all-files access permissions
        StoragePermissionsUtils.checkPermissions(this);
    }
}
