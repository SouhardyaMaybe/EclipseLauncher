package me.shadow.eclipselauncher.pojav;

import static me.shadow.eclipselauncher.launch.LaunchGame.preLaunch;
import static me.shadow.eclipselauncher.pojav.Tools.currentDisplayMetrics;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.palette.graphics.Palette;

import me.shadow.eclipselauncher.mcgui.ProgressLayout;
import me.shadow.eclipselauncher.anim.AnimPlayer;
import me.shadow.eclipselauncher.anim.animations.Animations;
import me.shadow.eclipselauncher.InfoDistributor;
import me.shadow.eclipselauncher.R;
import me.shadow.eclipselauncher.context.ContextExecutor;
import me.shadow.eclipselauncher.databinding.ActivityLauncherBinding;
import me.shadow.eclipselauncher.event.single.LaunchGameEvent;
import me.shadow.eclipselauncher.event.single.MainBackgroundChangeEvent;
import me.shadow.eclipselauncher.event.single.PageOpacityChangeEvent;
import me.shadow.eclipselauncher.event.single.SwapToLoginEvent;
import me.shadow.eclipselauncher.event.sticky.MinecraftVersionValueEvent;
import me.shadow.eclipselauncher.event.value.AddFragmentEvent;
import me.shadow.eclipselauncher.event.value.DownloadProgressKeyEvent;
import me.shadow.eclipselauncher.event.value.InstallGameEvent;
import me.shadow.eclipselauncher.event.value.InstallLocalModpackEvent;
import me.shadow.eclipselauncher.event.value.LocalLoginEvent;
import me.shadow.eclipselauncher.event.value.MicrosoftLoginEvent;
import me.shadow.eclipselauncher.event.value.OtherLoginEvent;
import me.shadow.eclipselauncher.feature.accounts.AccountType;
import me.shadow.eclipselauncher.feature.accounts.AccountsManager;
import me.shadow.eclipselauncher.feature.accounts.LocalAccountUtils;
import me.shadow.eclipselauncher.feature.background.BackgroundManager;
import me.shadow.eclipselauncher.feature.background.BackgroundType;
import me.shadow.eclipselauncher.feature.download.item.ModLoaderWrapper;
import me.shadow.eclipselauncher.feature.log.Logging;
import me.shadow.eclipselauncher.feature.mod.modpack.install.InstallExtra;
import me.shadow.eclipselauncher.feature.mod.modpack.install.InstallLocalModPack;
import me.shadow.eclipselauncher.feature.mod.modpack.install.ModPackInfo;
import me.shadow.eclipselauncher.feature.mod.modpack.install.ModPackUtils;
import me.shadow.eclipselauncher.feature.notice.CheckNewNotice;
import me.shadow.eclipselauncher.feature.notice.NoticeInfo;
import me.shadow.eclipselauncher.feature.update.UpdateUtils;
import me.shadow.eclipselauncher.feature.version.Version;
import me.shadow.eclipselauncher.feature.version.VersionsManager;
import me.shadow.eclipselauncher.feature.version.install.GameInstaller;
import me.shadow.eclipselauncher.feature.version.install.InstallTask;
import me.shadow.eclipselauncher.plugins.renderer.RendererPlugin;
import me.shadow.eclipselauncher.plugins.renderer.RendererPluginManager;
import me.shadow.eclipselauncher.setting.AllSettings;
import me.shadow.eclipselauncher.task.Task;
import me.shadow.eclipselauncher.task.TaskExecutors;
import me.shadow.eclipselauncher.ui.activity.BaseActivity;
import me.shadow.eclipselauncher.ui.dialog.EditTextDialog;
import me.shadow.eclipselauncher.ui.dialog.TipDialog;
import me.shadow.eclipselauncher.ui.fragment.AccountFragment;
import me.shadow.eclipselauncher.ui.fragment.BaseFragment;
import me.shadow.eclipselauncher.ui.fragment.DownloadFragment;
import me.shadow.eclipselauncher.ui.fragment.DownloadModFragment;
import me.shadow.eclipselauncher.ui.fragment.SettingsFragment;
import me.shadow.eclipselauncher.ui.subassembly.settingsbutton.ButtonType;
import me.shadow.eclipselauncher.ui.subassembly.settingsbutton.SettingsButtonWrapper;
import me.shadow.eclipselauncher.ui.subassembly.view.DraggableViewWrapper;
import me.shadow.eclipselauncher.utils.StoragePermissionsUtils;
import me.shadow.eclipselauncher.utils.ZHTools;
import me.shadow.eclipselauncher.utils.anim.ViewAnimUtils;
import me.shadow.eclipselauncher.utils.file.FileTools;
import me.shadow.eclipselauncher.utils.image.ImageUtils;

import me.shadow.eclipselauncher.pojav.authenticator.microsoft.MicrosoftBackgroundLogin;
import me.shadow.eclipselauncher.pojav.contracts.OpenDocumentWithExtension;
import me.shadow.eclipselauncher.pojav.fragments.MainMenuFragment;
import me.shadow.eclipselauncher.pojav.prefs.LauncherPreferences;
import me.shadow.eclipselauncher.pojav.progresskeeper.ProgressKeeper;
import me.shadow.eclipselauncher.pojav.progresskeeper.TaskCountListener;
import me.shadow.eclipselauncher.pojav.services.ProgressServiceKeeper;
import me.shadow.eclipselauncher.pojav.tasks.AsyncVersionList;
import me.shadow.eclipselauncher.pojav.utils.NotificationUtils;
import me.shadow.eclipselauncher.pojav.value.MinecraftAccount;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.Future;

public class LauncherActivity extends BaseActivity {
    private final AnimPlayer noticeAnimPlayer = new AnimPlayer();
    public final ActivityResultLauncher<Object> modInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (uris) -> {
                if (uris != null) {
                    Tools.launchModInstaller(this, uris.get(0));
                }
            });

    private ActivityLauncherBinding binding;
    private SettingsButtonWrapper mSettingsButtonWrapper;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private NotificationManager mNotificationManager;
    private Future<?> checkNotice;

    /* Allows to switch from one button "type" to another */
    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            if (f instanceof MainMenuFragment) {
                mSettingsButtonWrapper.setButtonType(ButtonType.SETTINGS);
            } else {
                mSettingsButtonWrapper.setButtonType(ButtonType.HOME);
            }
        }
    };

    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        // Hide the notification that starts the game if there are tasks executing.
        // Prevents the user from trying to launch the game with tasks ongoing.
        if (taskCount > 0) {
            TaskExecutors.runInUIThread(() -> mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START));
        }
    };

    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;

    @Subscribe()
    public void event(PageOpacityChangeEvent event) {
        setPageOpacity(event.getProgress());
    }

    @Subscribe()
    public void event(MainBackgroundChangeEvent event) {
        refreshBackground();
        setPageOpacity(AllSettings.getPageOpacity().getValue());
    }

    @Subscribe()
    public void event(SwapToLoginEvent event) {
        Fragment currentFragment = getCurrentFragment();
        //If a visible fragment is present, jump to AccountFragment unless that is already the visible one
        if (currentFragment == null || getVisibleFragment(AccountFragment.TAG) != null) return;
        ZHTools.swapFragmentWithAnim(currentFragment, AccountFragment.class, AccountFragment.TAG, null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(LaunchGameEvent event) {
        if (binding.progressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }

        Version version = VersionsManager.INSTANCE.getCurrentVersion();
        if (version == null) {
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return;
        }

        if (AccountsManager.INSTANCE.getAllAccounts().isEmpty()) {
            Toast.makeText(this, R.string.account_no_saved_accounts, Toast.LENGTH_LONG).show();
            EventBus.getDefault().post(new SwapToLoginEvent());
            return;
        }

        RendererPlugin rendererPlugin = RendererPluginManager.getConfigurablePluginOrNull(version.getRenderer());
        if (rendererPlugin != null) {
            StoragePermissionsUtils.checkPermissions(
                    this,
                    R.string.generic_warning,
                    getString(R.string.permissions_storage_for_renderer_config, rendererPlugin.getDisplayName(), InfoDistributor.APP_NAME),
                    new StoragePermissionsUtils.PermissionGranted() {
                        @Override
                        public void granted() {
                            launchGame(version);
                        }

                        @Override
                        public void cancelled() {
                            launchGame(version);
                        }
                    }
            );
            return;
        }

        launchGame(version);
    }

    @Subscribe()
    public void event(MicrosoftLoginEvent event) {
        new MicrosoftBackgroundLogin(false, event.getUri().getQueryParameter("code")).performLogin(
                this, null,
                AccountsManager.INSTANCE.getDoneListener(),
                AccountsManager.INSTANCE.getErrorListener()
        );
    }

    @Subscribe()
    public void event(OtherLoginEvent event) {
        Task.runTask(() -> {
                    event.getAccount().save();
                    Logging.i("Account", "Saved the account : " + event.getAccount().username);
                    return null;
                }).onThrowable(e -> Logging.e("Account", "Failed to save the account : " + e))
                .finallyTask(() -> AccountsManager.INSTANCE.getDoneListener().onLoginDone(event.getAccount()))
                .execute();
    }

    @Subscribe()
    public void event(LocalLoginEvent event) {
        String userName = event.getUserName();
        MinecraftAccount localAccount = new MinecraftAccount();
        localAccount.username = userName;
        localAccount.accountType = AccountType.LOCAL.getType();
        try {
            localAccount.save();
            Logging.i("Account", "Saved the account : " + localAccount.username);
        } catch (IOException e) {
            Logging.e("Account", "Failed to save the account : " + e);
        }

        AccountsManager.INSTANCE.getDoneListener().onLoginDone(localAccount);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(InstallLocalModpackEvent event) {
        InstallExtra installExtra = event.getInstallExtra();
        if (!installExtra.startInstall) return;

        if (binding.progressLayout.hasProcesses()) {
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return;
        }

        File dirGameModpackFile = new File(installExtra.modpackPath);
        ModPackInfo info = ModPackUtils.determineModpack(dirGameModpackFile);
        if (info.getType() == ModPackUtils.ModPackEnum.UNKNOWN) {
            InstallLocalModPack.showUnSupportDialog(this);
        }

        String modPackName = info.getName() != null ? info.getName() : FileTools.getFileNameWithoutExtension(dirGameModpackFile);

        new EditTextDialog.Builder(this)
                .setTitle(R.string.version_install_new)
                .setEditText(modPackName)
                .setAsRequired()
                .setConfirmListener((editText, checked) -> {
                    String customName = editText.getText().toString();

                    if (FileTools.isFilenameInvalid(editText)) {
                        return false;
                    }

                    if (VersionsManager.INSTANCE.isVersionExists(customName, true)) {
                        editText.setError(getString(R.string.version_install_exists));
                        return false;
                    }

                    Task.runTask(() -> {
                        ModLoaderWrapper modLoaderWrapper = InstallLocalModPack.installModPack(this, info.getType(), dirGameModpackFile, customName);
                        if (modLoaderWrapper != null) {
                            InstallTask downloadTask = modLoaderWrapper.getDownloadTask();

                            if (downloadTask != null) {
                                runOnUiThread(() -> Toast.makeText(this, getString(R.string.modpack_prepare_mod_loader_installation), Toast.LENGTH_SHORT).show());

                                Logging.i("Install Version", "Installing ModLoader: " + modLoaderWrapper.getModLoaderVersion());
                                File file = downloadTask.run(customName);
                                if (file != null) {
                                    return new kotlin.Pair<>(modLoaderWrapper, file);
                                }
                            }
                        }
                        return null;
                    }).beforeStart(TaskExecutors.getAndroidUI(), () -> ProgressLayout.setProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.generic_waiting)).ended(filePair -> {
                        if (filePair != null) {
                            try {
                                ModPackUtils.startModLoaderInstall(filePair.getFirst(), LauncherActivity.this, filePair.getSecond(), customName);
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).onThrowable(TaskExecutors.getAndroidUI(), e -> Tools.showErrorRemote(this, R.string.modpack_install_download_failed, e))
                    .finallyTask(TaskExecutors.getAndroidUI(), () -> ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE))
                    .execute();

                    return true;
                }).showDialog();
    }

    @Subscribe()
    public void event(InstallGameEvent event) {
        new GameInstaller(this, event).installGame();
    }

    @Subscribe()
    public void event(DownloadProgressKeyEvent event) {
        if (event.getObserve()) {
            binding.progressLayout.observe(event.getProgressKey());
        } else {
            binding.progressLayout.unObserve(event.getProgressKey());
        }
    }

    @Subscribe()
    public synchronized void event(AddFragmentEvent event) {
        Fragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            try {
                AddFragmentEvent.FragmentActivityCallBack activityCallBack = event.getFragmentActivityCallback();
                if (activityCallBack != null) {
                    activityCallBack.callBack(currentFragment.requireActivity());
                }
                ZHTools.addFragment(
                        currentFragment,
                        event.getFragmentClass(),
                        event.getFragmentTag(),
                        event.getBundle()
                );
            } catch (Exception e) {
                Logging.e("LauncherActivity", "Failed attempt to jump to a new Fragment!", e);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLauncherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        processFragment();
        processViews();

        mRequestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if(!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable);
                        if(runnable != null) runnable.run();
                    }
                }
        );
        checkNotificationPermission();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        ProgressKeeper.addTaskCountListener(binding.progressLayout);

        new AsyncVersionList().getVersionList(versions -> EventBus.getDefault().postSticky(
                        new MinecraftVersionValueEvent(versions)),
                false
        );

        checkNotice();

        //Check for downloaded packages, or check for updates
        Task.runTask(() -> {
            UpdateUtils.checkDownloadedPackage(this, false, true);
            return null;
        }).execute();
    }

    private void processFragment() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getCurrentFragment();
                if (currentFragment instanceof BaseFragment && !((BaseFragment) currentFragment).onBackPressed()) {
                    //The fragment handled the back event itself
                    return;
                }

                //Exit the launcher when only one or no fragments remain on the stack
                if (getSupportFragmentManager().getBackStackEntryCount() <= 1) {
                    finish();
                } else {
                    getSupportFragmentManager().popBackStackImmediate();
                }
            }
        });

        FragmentManager fragmentManager = getSupportFragmentManager();
        //Add the main fragment when the stack is still empty
        if (fragmentManager.getBackStackEntryCount() < 1) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack(MainMenuFragment.TAG)
                    .add(R.id.container_fragment, MainMenuFragment.class, null, MainMenuFragment.TAG).commit();
        }
    }

    private void processViews() {
        refreshBackground();
        setPageOpacity(AllSettings.getPageOpacity().getValue());
        mSettingsButtonWrapper = new SettingsButtonWrapper(binding.settingButton);
        mSettingsButtonWrapper.setOnTypeChangeListener(type -> ViewAnimUtils.setViewAnim(binding.settingButton, Animations.Pulse));
        binding.downloadButton.setOnClickListener(v -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(binding.containerFragment.getId());
            if (fragment != null && !(fragment instanceof DownloadFragment || fragment instanceof DownloadModFragment)) {
                ViewAnimUtils.setViewAnim(binding.downloadButton, Animations.Pulse);
                ZHTools.swapFragmentWithAnim(fragment, DownloadFragment.class, DownloadFragment.TAG, null);
            }
        });
        binding.settingButton.setOnClickListener(v -> {
            ViewAnimUtils.setViewAnim(binding.settingButton, Animations.Pulse);
            Fragment fragment = getSupportFragmentManager().findFragmentById(binding.containerFragment.getId());
            if (fragment instanceof MainMenuFragment) {
                ZHTools.swapFragmentWithAnim(fragment, SettingsFragment.class, SettingsFragment.TAG, null);
            } else {
                // The setting button doubles as a home button now
                Tools.backToMainMenu(this);
            }
        });
        binding.appTitleText.setText(InfoDistributor.APP_NAME);
        binding.progressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        binding.progressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        binding.progressLayout.observe(ProgressLayout.INSTALL_RESOURCE);
        binding.progressLayout.observe(ProgressLayout.LOGIN_ACCOUNT);
        binding.progressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);

        binding.noticeGotButton.setOnClickListener(v -> {
            setNotice(false);
            AllSettings.getNoticeDefault().put(false).save();
        });
        new DraggableViewWrapper(binding.noticeLayout, new DraggableViewWrapper.AttributesFetcher() {
            @NonNull
            @Override
            public DraggableViewWrapper.ScreenPixels getScreenPixels() {
                return new DraggableViewWrapper.ScreenPixels(0, 0,
                        currentDisplayMetrics.widthPixels - binding.noticeLayout.getWidth(),
                        currentDisplayMetrics.heightPixels - binding.noticeLayout.getHeight());
            }

            @NonNull
            @Override
            public int[] get() {
                return new int[]{(int) binding.noticeLayout.getX(), (int) binding.noticeLayout.getY()};
            }

            @Override
            public void set(int x, int y) {
                binding.noticeLayout.setX(x);
                binding.noticeLayout.setY(y);
            }
        }).init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        setPageOpacity(AllSettings.getPageOpacity().getValue());
        VersionsManager.INSTANCE.refresh("LauncherActivity:onResume", false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.progressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(binding.progressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);

        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
        ContextExecutor.clearActivity();
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }

    private void launchGame(Version version) {
        LocalAccountUtils.checkUsageAllowed(new LocalAccountUtils.CheckResultListener() {
            @Override
            public void onUsageAllowed() {
                preLaunch(LauncherActivity.this, version);
            }

            @Override
            public void onUsageDenied() {
                if (!AllSettings.getLocalAccountReminders().getValue()) {
                    preLaunch(LauncherActivity.this, version);
                } else {
                    LocalAccountUtils.openDialog(LauncherActivity.this, checked -> {
                                LocalAccountUtils.saveReminders(checked);
                                preLaunch(LauncherActivity.this, version);
                            },
                            getString(R.string.account_no_microsoft_account) + getString(R.string.account_purchase_minecraft_account_tip),
                            R.string.account_continue_to_launch_the_game);
                }
            }
        });
    }

    private void checkNotice() {
        checkNotice = TaskExecutors.getDefault().submit(() -> CheckNewNotice.checkNewNotice(noticeInfo -> {
            if (checkNotice.isCancelled() || noticeInfo == null) {
                return;
            }
            //Show the notice bar when notifications are enabled or the notice numbering differs from the stored value
            if (AllSettings.getNoticeDefault().getValue() ||
                    (noticeInfo.numbering != AllSettings.getNoticeNumbering().getValue())) {
                TaskExecutors.runInUIThread(() -> setNotice(true));
                AllSettings.getNoticeDefault().put(true)
                        .put(AllSettings.getNoticeNumbering(), noticeInfo.numbering)
                        .save();
            }
        }));
    }

    private void setNotice(boolean show) {
        if (show) {
            NoticeInfo noticeInfo = CheckNewNotice.getNoticeInfo();
            if (noticeInfo != null) {
                binding.noticeGotButton.setClickable(true);

                binding.noticeTitleView.setText(noticeInfo.title);
                binding.noticeMessageView.setText(noticeInfo.content);
                binding.noticeDateView.setText(noticeInfo.date);

                Linkify.addLinks(binding.noticeMessageView, Linkify.WEB_URLS);
                binding.noticeMessageView.setMovementMethod(LinkMovementMethod.getInstance());

                noticeAnimPlayer.clearEntries();
                noticeAnimPlayer.apply(new AnimPlayer.Entry(binding.noticeLayout, Animations.BounceEnlarge))
                        .setOnStart(() -> binding.noticeLayout.setVisibility(View.VISIBLE))
                        .start();
            }
        } else {
            binding.noticeGotButton.setClickable(false);

            noticeAnimPlayer.clearEntries();
            noticeAnimPlayer.apply(new AnimPlayer.Entry(binding.noticeLayout, Animations.BounceShrink))
                    .setOnStart(() -> binding.noticeLayout.setVisibility(View.VISIBLE))
                    .setOnEnd(() -> binding.noticeLayout.setVisibility(View.GONE))
                    .start();
        }
    }

    private void refreshBackground() {
        BackgroundManager.setBackgroundImage(this, BackgroundType.MAIN_MENU, binding.backgroundView, this::refreshTopBarColor);
    }

    private void refreshTopBarColor(boolean loadFromBackground) {
        int backgroundMenuTop = ContextCompat.getColor(this, R.color.background_menu_top);

        if (loadFromBackground) {
            Bitmap bitmap = ImageUtils.getBitmapFromImageView(binding.backgroundView);
            if (bitmap != null) {
                Palette palette = Palette.from(bitmap).generate();

                boolean isDarkMode = ZHTools.isDarkMode(this);
                binding.topLayout.setBackgroundColor(
                        isDarkMode ?
                                palette.getDarkVibrantColor(backgroundMenuTop) :
                                palette.getLightVibrantColor(backgroundMenuTop)
                );

                int mutedColor = isDarkMode ?
                        palette.getLightMutedColor(0xFFFFFFFF) :
                        palette.getDarkMutedColor(0xFFFFFFFF);

                ColorStateList colorStateList = ColorStateList.valueOf(mutedColor);
                binding.appTitleText.setTextColor(mutedColor);
                binding.downloadButton.setImageTintList(colorStateList);
                binding.settingButton.setImageTintList(colorStateList);

                return;
            }
        }
        binding.topLayout.setBackgroundColor(backgroundMenuTop);
        binding.appTitleText.setTextColor(ContextCompat.getColor(this, R.color.menu_bar_text));
        ColorStateList colorStateList = ColorStateList.valueOf(0xFFFFFFFF);
        binding.downloadButton.setImageTintList(colorStateList);
        binding.settingButton.setImageTintList(colorStateList);
    }

    @SuppressWarnings("SameParameterValue")
    private Fragment getVisibleFragment(String tag) {
        return checkFragmentAvailability(getSupportFragmentManager().findFragmentByTag(tag));
    }

    private Fragment getVisibleFragment(int id) {
        return checkFragmentAvailability(getSupportFragmentManager().findFragmentById(id));
    }

    private Fragment getCurrentFragment() {
        return getVisibleFragment(binding.containerFragment.getId());
    }

    private Fragment checkFragmentAvailability(Fragment fragment) {
        if (fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }

    private void checkNotificationPermission() {
        if (AllSettings.getSkipNotificationPermissionCheck().getValue() || ZHTools.checkForNotificationPermission()) {
            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionReasoning();
            return;
        }
        askForNotificationPermission(null);
    }

    private void showNotificationPermissionReasoning() {
        new TipDialog.Builder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(getString(R.string.notification_permission_dialog_text, InfoDistributor.APP_NAME, InfoDistributor.APP_NAME))
                .setConfirmClickListener(checked -> askForNotificationPermission(null))
                .setCancelClickListener(this::handleNoNotificationPermission)
                .showDialog();
    }

    private void handleNoNotificationPermission() {
        AllSettings.getSkipNotificationPermissionCheck().put(true).save();
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show();
    }

    public void askForNotificationPermission(Runnable onSuccessRunnable) {
        if (Build.VERSION.SDK_INT < 33) return;
        if (onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void setPageOpacity(int pageOpacity) {
        BigDecimal opacity = BigDecimal.valueOf(pageOpacity).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        float v = opacity.floatValue();

        binding.containerFragment.setAlpha(v);

        BigDecimal adjustedOpacity = BackgroundManager.hasBackgroundImage(BackgroundType.MAIN_MENU)
                ? opacity.subtract(BigDecimal.valueOf(0.1)).max(BigDecimal.ZERO)
                : BigDecimal.ONE;

        binding.topLayout.setAlpha(adjustedOpacity.floatValue());
    }
}
