package me.shadow.eclipselauncher.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.InfoDistributor
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.ActivitySplashBinding
import me.shadow.eclipselauncher.feature.unpack.Components
import me.shadow.eclipselauncher.feature.unpack.Jre
import me.shadow.eclipselauncher.feature.unpack.UnpackComponentsTask
import me.shadow.eclipselauncher.feature.unpack.UnpackJreTask
import me.shadow.eclipselauncher.feature.unpack.UnpackSingleFilesTask
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.ui.fragment.PaneSwitcher
import me.shadow.eclipselauncher.utils.StoragePermissionsUtils
import me.shadow.eclipselauncher.pojav.LauncherActivity
import me.shadow.eclipselauncher.pojav.MissingStorageActivity
import me.shadow.eclipselauncher.pojav.Tools

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private var paneSwitcher: PaneSwitcher? = null
    private var isStarted: Boolean = false
    private lateinit var binding: ActivitySplashBinding
    private lateinit var installableAdapter: InstallableAdapter
    private val items: MutableList<InstallableItem> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initItems()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Portrait single-pane mode: the start pane takes over, the component list stays landscape-only
        paneSwitcher = PaneSwitcher(binding.root, R.id.recycler_view, R.id.operate_layout, 0, PaneSwitcher.Mode.PRIMARY_RIGHT)
        paneSwitcher?.applyOrientation(resources.configuration)

        binding.titleText.text = InfoDistributor.APP_NAME
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SplashActivity)
            adapter = installableAdapter
        }

        binding.startButton.apply {
            setOnClickListener {
                if (isStarted) return@setOnClickListener
                isStarted = true
                binding.splashText.setText(R.string.splash_screen_installing)
                installableAdapter.startAllTasks()
            }
            isClickable = false
        }

        if (!Tools.checkStorageRoot()) {
            startActivity(Intent(this, MissingStorageActivity::class.java))
            finish()
            return
        }

        // On Android 9 or below, check the storage permission (not all-files access); holding it keeps files and folders creating normally
        // However the permission is not forced; if the user declines, any later problems are their own to bear
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !StoragePermissionsUtils.hasStoragePermissions(this)) {
            TipDialog.Builder(this)
                .setTitle(R.string.generic_warning)
                .setMessage(InfoCenter.replaceName(this, R.string.permissions_write_external_storage))
                .setWarning()
                .setConfirmClickListener { requestStoragePermissions() }
                .setCancelClickListener { checkEnd() } // The user cancelled, so respect their choice
                .showDialog()
        } else {
            checkEnd()
        }
    }

    private fun requestStoragePermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            // Finish the check whether or not the permission was granted; the launcher does not require it
            // But if a storage-permission problem occurs later, the user bears the consequences
            checkEnd()
        }
    }

    private fun initItems() {
        Components.entries.forEach {
            val unpackComponentsTask = UnpackComponentsTask(this, it)
            if (!unpackComponentsTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.displayName,
                        it.summary?.let { it1 -> getString(it1) },
                        unpackComponentsTask
                    )
                )
            }
        }
        Jre.entries.forEach {
            val unpackJreTask = UnpackJreTask(this, it)
            if (!unpackJreTask.isCheckFailed()) {
                items.add(
                    InstallableItem(
                        it.jreName,
                        getString(it.summary),
                        unpackJreTask
                    )
                )
            }
        }
        items.sort()
        installableAdapter = InstallableAdapter(items) {
            toMain()
        }
    }
    
    private fun checkEnd() {
        installableAdapter.checkAllTask()
        Task.runTask {
            UnpackSingleFilesTask(this).run()
        }.execute()

        binding.startButton.isClickable = true
    }

    private fun toMain() {
        startActivity(Intent(this, LauncherActivity::class.java))
        finish()
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE: Int = 100
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        paneSwitcher?.applyOrientation(newConfig)
    }
}
