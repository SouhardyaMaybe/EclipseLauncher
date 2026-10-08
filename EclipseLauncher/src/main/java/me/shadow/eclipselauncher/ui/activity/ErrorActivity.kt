package me.shadow.eclipselauncher.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import me.shadow.eclipselauncher.InfoCenter
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.ActivityErrorBinding
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.pojav.Tools

class ErrorActivity : BaseActivity() {
    private lateinit var binding: ActivityErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras
        extras ?: run {
            finish()
            return
        }

        binding.errorConfirm.setOnClickListener { finish() }
        binding.errorRestart.setOnClickListener {
            startActivity(Intent(this@ErrorActivity, SplashActivity::class.java))
        }
        binding.shareLog.setOnClickListener { ZHTools.shareLogs(this) }

        if (extras.getBoolean(BUNDLE_IS_LAUNCHER_CRASH, false)) {
            showLauncherCrash(extras)
            return
        }
        if (extras.getBoolean(BUNDLE_IS_GAME_CRASH, false)) {
            // Not an app crash, so block screenshots of this page
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            showGameCrash(extras)
            return
        }
        finish()
    }

    private fun showLauncherCrash(extras: Bundle) {
        val context = this

        val throwable = extras.getSerializable(BUNDLE_THROWABLE) as Throwable?
        val stackTrace = if (throwable != null) Tools.printToString(throwable) else "<null>"
        val strSavePath = extras.getString(BUNDLE_SAVE_PATH)
        val errorText = "$strSavePath :\r\n\r\n$stackTrace"

        binding.apply {
            this.errorTitle.text = InfoCenter.replaceName(context, R.string.error_fatal)
            this.errorText.text = errorText

            this.topView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_menu_top_error))
            this.background.setBackgroundColor(ContextCompat.getColor(context, R.color.background_app_error))
        }
    }

    private fun showGameCrash(extras: Bundle) {
        val code = extras.getInt(BUNDLE_CODE, 0)
        if (code == 0) {
            finish()
            return
        }
        val errorText = if (extras.getBoolean(BUNDLE_IS_SIGNAL)) R.string.game_singnal_message else R.string.game_exit_message

        val context = this

        binding.apply {
            this.errorTitle.setText(R.string.generic_wrong_tip)
            this.errorText.apply {
                text = getString(errorText, code)
                textSize = 14f
            }
            this.errorTip.visibility = View.VISIBLE
            this.errorNoScreenshot.visibility = View.VISIBLE

            this.topView.setBackgroundColor(ContextCompat.getColor(context, R.color.background_menu_top))
            this.background.setBackgroundColor(ContextCompat.getColor(context, R.color.background_app))
        }
    }

    companion object {
        private const val BUNDLE_IS_LAUNCHER_CRASH = "is_launcher_crash"
        private const val BUNDLE_IS_GAME_CRASH = "is_game_crash"
        private const val BUNDLE_IS_SIGNAL = "is_signal"
        private const val BUNDLE_CODE = "code"
        private const val BUNDLE_THROWABLE = "throwable"
        private const val BUNDLE_SAVE_PATH = "save_path"

        @JvmStatic
        fun showLauncherCrash(ctx: Context, savePath: String?, th: Throwable?) {
            val intent = Intent(ctx, ErrorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(BUNDLE_THROWABLE, th)
            intent.putExtra(BUNDLE_SAVE_PATH, savePath)
            intent.putExtra(BUNDLE_IS_LAUNCHER_CRASH, true)
            ctx.startActivity(intent)
        }

        @JvmStatic
        fun showExitMessage(
            ctx: Context,
            code: Int,
            isSignal: Boolean
        ) {
            val intent = Intent(ctx, ErrorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(BUNDLE_CODE, code)
            intent.putExtra(BUNDLE_IS_LAUNCHER_CRASH, false)
            intent.putExtra(BUNDLE_IS_SIGNAL, isSignal)
            intent.putExtra(BUNDLE_IS_GAME_CRASH, true)
            ctx.startActivity(intent)
        }

    }
}
