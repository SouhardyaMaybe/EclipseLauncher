package me.shadow.eclipselauncher.core

import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout

import androidx.activity.OnBackPressedCallback
import androidx.drawerlayout.widget.DrawerLayout

import me.shadow.eclipselauncher.databinding.ActivityCustomControlsBinding
import me.shadow.eclipselauncher.databinding.ViewControlMenuBinding
import me.shadow.eclipselauncher.feature.background.BackgroundManager
import me.shadow.eclipselauncher.feature.background.BackgroundType
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.activity.BaseActivity
import me.shadow.eclipselauncher.ui.subassembly.menu.ControlMenu
import me.shadow.eclipselauncher.ui.subassembly.view.GameMenuViewWrapper

import me.shadow.eclipselauncher.core.customcontrols.ControlLayout
import me.shadow.eclipselauncher.core.customcontrols.EditorExitable

import java.io.IOException

class CustomControlsActivity : BaseActivity(), EditorExitable {
    private lateinit var binding: ActivityCustomControlsBinding
    private var mControlPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        parseBundle()
        binding = ActivityCustomControlsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val controlLayout: ControlLayout = binding.customctrlControllayout
        val drawerLayout: DrawerLayout = binding.customctrlDrawerlayout
        val drawerNavigationView: FrameLayout = binding.customctrlNavigationView

        GameMenuViewWrapper(this, View.OnClickListener {
            val open = drawerLayout.isDrawerOpen(drawerNavigationView)

            if (open) drawerLayout.closeDrawer(drawerNavigationView)
            else drawerLayout.openDrawer(drawerNavigationView)
        }, false).setVisibility(true)

        BackgroundManager.setBackgroundImage(this, BackgroundType.CUSTOM_CONTROLS, binding.backgroundView, null)

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        drawerLayout.setScrimColor(Color.TRANSPARENT)

        val controlMenuBinding = ViewControlMenuBinding.inflate(layoutInflater)
        ControlMenu(this, this, controlMenuBinding, controlLayout, true)

        drawerNavigationView.addView(controlMenuBinding.root)
        controlLayout.setModifiable(true)
        try {
            if (mControlPath == null) controlLayout.loadLayout(null as String?)
            else controlLayout.loadLayout(mControlPath)
        } catch (e: IOException) {
            Tools.showError(this, e)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                binding.customctrlControllayout.askToExit(this@CustomControlsActivity)
            }
        })
    }

    private fun parseBundle() {
        val bundle = intent.extras
        if (bundle != null) {
            mControlPath = bundle.getString(BUNDLE_CONTROL_PATH)
        }
    }

    override fun shouldIgnoreNotch(): Boolean {
        return AllSettings.ignoreNotch.getValue()
    }

    override fun exitEditor() {
        finish()
    }

    companion object {
        const val BUNDLE_CONTROL_PATH = "control_path"
    }
}
