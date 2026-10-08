package me.shadow.eclipselauncher.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.EditText
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.angcyo.tablayout.DslTabLayout
import me.shadow.eclipselauncher.anim.AnimPlayer
import me.shadow.eclipselauncher.anim.animations.Animations
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.FragmentAccountBinding
import me.shadow.eclipselauncher.databinding.ItemOtherServerBinding
import me.shadow.eclipselauncher.databinding.ViewAddOtherServerBinding
import me.shadow.eclipselauncher.databinding.ViewSingleActionPopupBinding
import me.shadow.eclipselauncher.event.single.AccountUpdateEvent
import me.shadow.eclipselauncher.event.value.LocalLoginEvent
import me.shadow.eclipselauncher.event.value.OtherLoginEvent
import me.shadow.eclipselauncher.feature.accounts.AccountUtils
import me.shadow.eclipselauncher.feature.accounts.AccountsManager
import me.shadow.eclipselauncher.feature.accounts.LocalAccountUtils
import me.shadow.eclipselauncher.feature.accounts.LocalAccountUtils.CheckResultListener
import me.shadow.eclipselauncher.feature.accounts.LocalAccountUtils.Companion.checkUsageAllowed
import me.shadow.eclipselauncher.feature.accounts.LocalAccountUtils.Companion.openDialog
import me.shadow.eclipselauncher.feature.accounts.OtherLoginHelper
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.feature.login.OtherLoginApi
import me.shadow.eclipselauncher.feature.login.Servers
import me.shadow.eclipselauncher.feature.login.Servers.Server
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.task.Task
import me.shadow.eclipselauncher.task.TaskExecutors
import me.shadow.eclipselauncher.ui.dialog.EditTextDialog
import me.shadow.eclipselauncher.ui.dialog.OtherLoginDialog
import me.shadow.eclipselauncher.ui.dialog.TipDialog
import me.shadow.eclipselauncher.ui.layout.AnimRelativeLayout
import me.shadow.eclipselauncher.ui.subassembly.account.AccountAdapter
import me.shadow.eclipselauncher.ui.subassembly.account.AccountAdapter.AccountUpdateListener
import me.shadow.eclipselauncher.ui.subassembly.account.SelectAccountListener
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.http.NetworkUtils
import me.shadow.eclipselauncher.utils.path.PathManager
import me.shadow.eclipselauncher.utils.skin.UserCosmetics
import me.shadow.eclipselauncher.utils.stringutils.StringUtils
import me.shadow.eclipselauncher.pojav.Tools
import me.shadow.eclipselauncher.pojav.fragments.MicrosoftLoginFragment
import me.shadow.eclipselauncher.pojav.value.MinecraftAccount
import org.apache.commons.io.FileUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern


class AccountFragment : FragmentWithAnim(R.layout.fragment_account), View.OnClickListener {
    companion object {
        const val TAG = "AccountFragment"
    }

    private lateinit var binding: FragmentAccountBinding

    // Skins and capes chosen while the offline-account dialog is still open;
    // they are applied to the account as soon as it has been created
    private var pendingSkin: Bitmap? = null
    private var pendingCape: Bitmap? = null
    private var mLoginDialog: EditTextDialog? = null

    private lateinit var pickDialogSkin: ActivityResultLauncher<Array<String>>
    private lateinit var pickDialogCape: ActivityResultLauncher<Array<String>>
    private lateinit var pickPaneSkin: ActivityResultLauncher<Array<String>>
    private lateinit var pickPaneCape: ActivityResultLauncher<Array<String>>

    private val mAccountsData: MutableList<MinecraftAccount> = AccountsManager.allAccounts.toMutableList()
    private val mAccountAdapter = AccountAdapter(mAccountsData)

    private val selectAccountListener = object : SelectAccountListener {
        override fun onSelect(account: MinecraftAccount) {
            if (!isTaskRunning()) {
                AccountsManager.currentAccount = account
            } else {
                TaskExecutors.runInUIThread {
                    Toast.makeText(
                        requireActivity(),
                        R.string.tasks_ongoing,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private val mServerActionPopupWindow: PopupWindow = PopupWindow().apply {
        isFocusable = true
        isOutsideTouchable = true
    }

    private val mLocalNamePattern = Pattern.compile("[^a-zA-Z0-9_]")
    private var mOtherServerConfig: Servers? = null
    private val mOtherServerConfigFile = File(PathManager.DIR_GAME_HOME, "servers.json")
    private val mOtherServerList: MutableList<Server> = ArrayList()
    private val mOtherServerViewList: MutableList<View> = ArrayList()

    private lateinit var mProgressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickDialogSkin = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { uri ->
            onTexturePicked(uri, forDialog = true, skin = true)
        }
        pickDialogCape = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { uri ->
            onTexturePicked(uri, forDialog = true, skin = false)
        }
        pickPaneSkin = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { uri ->
            onTexturePicked(uri, forDialog = false, skin = true)
        }
        pickPaneCape = registerForActivityResult<Array<String>, Uri>(ActivityResultContracts.OpenDocument()) { uri ->
            onTexturePicked(uri, forDialog = false, skin = false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(layoutInflater)
        mProgressDialog = ZHTools.createTaskRunningDialog(binding.root.context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireActivity()

        mAccountAdapter.setAccountUpdateListener(object : AccountUpdateListener {
            override fun onViewClick(account: MinecraftAccount) {
                selectAccountListener.onSelect(account)
            }

            override fun onRefresh(account: MinecraftAccount) {
                if (!isTaskRunning()) {
                    if (!NetworkUtils.isNetworkAvailable(context)) {
                        Toast.makeText(context, R.string.account_login_no_network, Toast.LENGTH_SHORT).show()
                        return
                    }
                    AccountsManager.performLogin(context, account)
                } else {
                    Toast.makeText(context, R.string.tasks_ongoing, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDelete(account: MinecraftAccount) {
                TipDialog.Builder(context)
                    .setTitle(R.string.generic_warning)
                    .setMessage(R.string.account_remove)
                    .setConfirm(R.string.generic_delete)
                    .setWarning()
                    .setConfirmClickListener {
                        val accountFile =
                            File(PathManager.DIR_ACCOUNT_NEW, account.uniqueUUID)
                        val userSkinFile = UserCosmetics.skinFile(account)
                        val userCapeFile = UserCosmetics.capeFile(account)
                        if (accountFile.exists()) FileUtils.deleteQuietly(accountFile)
                        if (userSkinFile.exists()) FileUtils.deleteQuietly(userSkinFile)
                        if (userCapeFile.exists()) FileUtils.deleteQuietly(userCapeFile)
                        reloadAccounts()
                    }.showDialog()
            }
        })

        binding.apply {
            accountsRecycler.layoutManager = LinearLayoutManager(context)
            accountsRecycler.setLayoutAnimation(
                LayoutAnimationController(
                    AnimationUtils.loadAnimation(
                        context,
                        R.anim.fade_downwards
                    )
                )
            )
            accountsRecycler.adapter = mAccountAdapter

            setSkinButton.setOnClickListener {
                if (AccountsManager.currentAccount == null) {
                    Toast.makeText(context, R.string.account_select_account_first, Toast.LENGTH_SHORT).show()
                } else {
                    pickPaneSkin.launch(arrayOf("image/*"))
                }
            }
            setCapeButton.setOnClickListener {
                if (AccountsManager.currentAccount == null) {
                    Toast.makeText(context, R.string.account_select_account_first, Toast.LENGTH_SHORT).show()
                } else {
                    pickPaneCape.launch(arrayOf("image/*"))
                }
            }
            deleteSkinButton.setOnClickListener { deleteTexture(skin = true) }
            deleteCapeButton.setOnClickListener { deleteTexture(skin = false) }

            accountTypeTab.observeIndexChange { _, toIndex, _, fromUser ->
                fun nonMicrosoftLogin(message: Int, login: () -> Unit) {
                    checkUsageAllowed(object : CheckResultListener {
                        override fun onUsageAllowed() {
                            login()
                        }

                        override fun onUsageDenied() {
                            if (!AllSettings.localAccountReminders.getValue()) {
                                login()
                            } else {
                                openDialog(
                                    context,
                                    TipDialog.OnConfirmClickListener { checked ->
                                        LocalAccountUtils.saveReminders(checked)
                                        login()
                                    },
                                    getString(message) + getString(
                                        R.string.account_purchase_minecraft_account_tip
                                    ),
                                    R.string.account_no_microsoft_account_continue
                                )
                            }
                        }
                    })
                }

                if (fromUser) { // Only respond to manual clicks, otherwise the Microsoft login screen would keep opening
                    when (toIndex) {
                        // Microsoft account
                        0 -> ZHTools.swapFragmentWithAnim(
                            this@AccountFragment,
                            MicrosoftLoginFragment::class.java,
                            MicrosoftLoginFragment.TAG,
                            null
                        )
                        // Offline account
                        1 -> {
                            nonMicrosoftLogin(
                                R.string.account_no_microsoft_account_local
                            ) { localLogin() }
                        }
                        // External account
                        else -> {
                            nonMicrosoftLogin(
                                R.string.account_no_microsoft_account_other
                            ) { otherLogin(toIndex - 2) /* Server indexes start at 0 */ }
                        }
                    }
                }
            }

            addServer.setOnClickListener(this@AccountFragment)
            returnButton.setOnClickListener(this@AccountFragment)
        }

        refreshSkinPane()
        reloadAccounts()
        refreshOtherServer()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun reloadRecyclerView() {
        this.mAccountsData.clear()
        mAccountsData.addAll(AccountsManager.allAccounts)

        this.mAccountAdapter.notifyDataSetChanged()
        binding.accountsRecycler.scheduleLayoutAnimation()
    }

    private fun reloadAccounts() {
        Task.runTask {
            AccountsManager.reload()
        }.ended(TaskExecutors.getAndroidUI()) {
            reloadRecyclerView()
            refreshSkinPane()
        }.execute()
    }

    /** Refresh the operate pane: name, type, model preview and texture button states */
    private fun refreshSkinPane() {
        val account = AccountsManager.currentAccount
        binding.apply {
            userName.text = account?.username
            accountType.text = account?.let { AccountUtils.getAccountTypeName(requireContext(), it) }
            accountType.visibility = if (account == null) View.GONE else View.VISIBLE

            val enabled = account != null
            setSkinButton.alpha = if (enabled) 1f else 0.45f
            deleteSkinButton.alpha = if (enabled) 1f else 0.45f
            setCapeButton.alpha = if (enabled) 1f else 0.45f
            deleteCapeButton.alpha = if (enabled) 1f else 0.45f

            // Without a custom texture the model falls back to the bundled skin
            playerModelView.setSkin(account?.let { UserCosmetics.load(UserCosmetics.skinFile(it)) })
            playerModelView.setCape(account?.let { UserCosmetics.load(UserCosmetics.capeFile(it)) })
        }
    }

    /** Store a picked skin or cape either for the pending dialog account or the selected account */
    private fun onTexturePicked(uri: Uri?, forDialog: Boolean, skin: Boolean) {
        if (uri == null) return
        val texture = UserCosmetics.read(requireContext(), uri)
        if (texture == null) {
            Toast.makeText(requireContext(), R.string.account_texture_invalid, Toast.LENGTH_SHORT).show()
            return
        }

        if (forDialog) {
            if (skin) {
                pendingSkin = texture
                mLoginDialog?.markSkinSelected(true)
            } else {
                pendingCape = texture
                mLoginDialog?.markCapeSelected(true)
            }
            return
        }

        val account = AccountsManager.currentAccount
        if (account == null) {
            Toast.makeText(requireContext(), R.string.account_select_account_first, Toast.LENGTH_SHORT).show()
            return
        }
        val file = if (skin) UserCosmetics.skinFile(account) else UserCosmetics.capeFile(account)
        runCatching { UserCosmetics.write(texture, file) }.onFailure { e ->
            Logging.e("AccountFragment", "Failed to save the picked texture", e)
            Toast.makeText(requireContext(), R.string.account_texture_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        EventBus.getDefault().post(AccountUpdateEvent())
    }

    /** Delete the stored custom texture of the currently selected account */
    private fun deleteTexture(skin: Boolean) {
        val account = AccountsManager.currentAccount
        if (account == null) {
            Toast.makeText(requireContext(), R.string.account_select_account_first, Toast.LENGTH_SHORT).show()
            return
        }
        val file = if (skin) UserCosmetics.skinFile(account) else UserCosmetics.capeFile(account)
        if (file.exists()) {
            FileUtils.deleteQuietly(file)
            EventBus.getDefault().post(AccountUpdateEvent())
        }
    }

    private fun SpannableString.spanText(start: Int, end: Int, what: Any) {
        this.setSpan(what, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun localLogin() {
        pendingSkin = null
        pendingCape = null

        fun startLogin(name: String) {
            val trimmed = name.trim()
            val known = AccountsManager.allAccounts.map { it.uniqueUUID }.toSet()
            EventBus.getDefault().post(LocalLoginEvent(trimmed))

            // The account exists by now: move any picked skin and cape onto it
            val skin = pendingSkin
            val cape = pendingCape
            pendingSkin = null
            pendingCape = null
            if (skin != null || cape != null) {
                val account = AccountsManager.allAccounts.firstOrNull { it.uniqueUUID !in known }
                    ?: AccountsManager.allAccounts.firstOrNull { it.username == trimmed }
                if (account != null) {
                    runCatching {
                        skin?.let { UserCosmetics.write(it, UserCosmetics.skinFile(account)) }
                        cape?.let { UserCosmetics.write(it, UserCosmetics.capeFile(account)) }
                    }.onFailure { e ->
                        Logging.e("AccountFragment", "Failed to save the picked texture", e)
                    }
                }
                EventBus.getDefault().post(AccountUpdateEvent())
            }
        }

        val dialog = EditTextDialog.Builder(requireActivity())
            .setTitle(R.string.account_login_local_name)
            .setConfirmText(R.string.generic_login)
            .setEmptyErrorText(R.string.account_local_account_empty)
            .setAsRequired()
            .setShowSkinCape(true)
            .setSkinListener { pickDialogSkin.launch(arrayOf("image/*")) }
            .setCapeListener { pickDialogCape.launch(arrayOf("image/*")) }
            .setConfirmListener { editText, _ ->
                val string = editText.text.toString()
                if (string.length <= 2 || string.length > 16 || mLocalNamePattern.matcher(string).find()) {
                    TipDialog.Builder(requireContext())
                        .setTitle(R.string.generic_warning)
                        .setMessage(R.string.account_local_account_invalid)
                        .setWarning()
                        .setTextBeautifier { _, messageText ->
                            val text = messageText.text.toString()
                            val startTag = "[RED;BOLD]"
                            val endTag = "[/RED;BOLD]"

                            val startIndex = text.indexOf(startTag)
                            val endIndex = text.indexOf(endTag)

                            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                                val styledText = text.substring(startIndex + startTag.length, endIndex)
                                val plainText = text.replace(startTag, "").replace(endTag, "")
                                val adjustedEndIndex = startIndex + styledText.length

                                val spannableString = SpannableString(plainText)
                                spannableString.spanText(startIndex, adjustedEndIndex, ForegroundColorSpan(Color.RED))
                                spannableString.spanText(startIndex, adjustedEndIndex, StyleSpan(Typeface.BOLD))

                                messageText.text = spannableString
                            }
                        }
                        .setCenterMessage(false)
                        .setConfirmClickListener { startLogin(string) }
                        .setCancelable(false)
                        .setConfirmButtonCountdown(3000L)
                        .showDialog()
                } else startLogin(string)

                true
            }.buildDialog()

        mLoginDialog = dialog
        dialog.show()
    }

    private fun otherLogin(index: Int) {
        val server = mOtherServerList[index]
        OtherLoginDialog(requireActivity(), server,
            object : OtherLoginHelper.OnLoginListener {
                override fun onLoading() {
                    mProgressDialog.show()
                }

                override fun unLoading() {
                    mProgressDialog.dismiss()
                }

                override fun onSuccess(account: MinecraftAccount) {
                    EventBus.getDefault().post(OtherLoginEvent(account))
                }

                override fun onFailed(error: String) {
                    mProgressDialog.dismiss()

                    TipDialog.Builder(requireActivity())
                        .setTitle(R.string.generic_warning)
                        .setMessage(getString(R.string.other_login_error) + error)
                        .setWarning()
                        .setCancel(android.R.string.copy)
                        .setCancelClickListener {
                            StringUtils.copyText(
                                "error",
                                error,
                                requireActivity()
                            )
                        }
                        .showDialog()
                }
            }).show()
    }

    private fun refreshOtherServer() {
        Task.runTask {
            mOtherServerList.clear()
            if (mOtherServerConfigFile.exists()) {
                runCatching {
                    val serverConfig = Tools.GLOBAL_GSON.fromJson(
                        Tools.read(mOtherServerConfigFile.absolutePath),
                        Servers::class.java
                    )
                    mOtherServerConfig = serverConfig
                    serverConfig.server.forEach { server ->
                        mOtherServerList.add(server)
                    }
                }
            }
        }.ended(TaskExecutors.getAndroidUI()) {
            // Add the external servers to the account type tab
            mOtherServerViewList.forEach { view ->
                binding.accountTypeTab.removeView(view)
            }
            mOtherServerViewList.clear()

            val activity = requireActivity()
            val layoutInflater = activity.layoutInflater

            fun createView(server: Server): AnimRelativeLayout {
                val p8 = Tools.dpToPx(8f).toInt()
                val view = ItemOtherServerBinding.inflate(layoutInflater)
                view.text.text = server.serverName
                view.root.setOnLongClickListener { v ->
                    refreshActionPopupWindow(v, ViewSingleActionPopupBinding.inflate(LayoutInflater.from(activity)).apply {
                        icon.setImageDrawable(
                            ContextCompat.getDrawable(requireActivity(), R.drawable.ic_menu_delete_forever)
                        )
                        text.setText(R.string.generic_delete)
                        text.setOnClickListener {
                            deleteOtherServer(server)
                            mServerActionPopupWindow.dismiss()
                        }
                    })
                    true
                }
                view.root.layoutParams = DslTabLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                return view.root.apply {
                    setPadding(p8, 0, p8, 0)
                }
            }

            mOtherServerList.forEach { server ->
                val view = createView(server)
                mOtherServerViewList.add(view)
                binding.accountTypeTab.addView(view)
            }
        }.execute()
    }

    private fun showServerTypeSelectDialog(stringId: Int, type: Int) {
        EditTextDialog.Builder(requireActivity())
            .setTitle(stringId)
            .setAsRequired()
            .setConfirmListener { editText, _ ->
                addOtherServer(editText, type)
                true
            }.showDialog()
    }

    private fun checkServerConfig() {
        mOtherServerConfig ?: run {
            val servers = Servers()
            servers.server = ArrayList()
            mOtherServerConfig = servers
        }
    }

    private fun addOtherServer(editText: EditText, type: Int) {
        Task.runTask {
            val editString = editText.text.toString()
            val serverUrl =
                if (type == 0) AccountUtils.tryGetFullServerUrl(editString) else editString
            OtherLoginApi.getServeInfo(
                requireActivity(),
                if (type == 0) serverUrl else "https://auth.mc-user.com:233/$serverUrl"
            )?.let { data ->
                val server = Server()
                JSONObject(data).optJSONObject("meta")?.let { meta ->
                    server.serverName = meta.optString("serverName")
                    server.baseUrl = serverUrl
                    if (type == 0) {
                        server.register =
                            meta.optJSONObject("links")?.optString("register") ?: ""
                    } else {
                        server.baseUrl = "https://auth.mc-user.com:233/$serverUrl"
                        server.register = "https://login.mc-user.com:233/$serverUrl"
                    }
                    checkServerConfig()
                    mOtherServerConfig?.server?.apply addServer@{
                        forEach {
                            // Make sure servers are not added twice
                            if (it.baseUrl == server.baseUrl) return@addServer
                        }
                        add(server)
                    }
                    Tools.write(
                        mOtherServerConfigFile.absolutePath,
                        Tools.GLOBAL_GSON.toJson(mOtherServerConfig, Servers::class.java)
                    )
                }
            }
        }.beforeStart(TaskExecutors.getAndroidUI()) {
            mProgressDialog.show()
        }.ended(TaskExecutors.getAndroidUI()) {
            refreshOtherServer()
            mProgressDialog.dismiss()
        }.onThrowable { e ->
            Logging.e("Add Other Server", Tools.printToString(e))
        }.execute()
    }

    private fun deleteOtherServer(server: Server) {
        TipDialog.Builder(requireActivity())
            .setTitle(getString(R.string.account_remove_login_type_title, server.serverName))
            .setMessage(R.string.account_remove_login_type_message)
            .setWarning()
            .setConfirmClickListener {
                checkServerConfig()
                mOtherServerConfig?.server?.remove(server)
                Tools.write(
                    mOtherServerConfigFile.absolutePath,
                    Tools.GLOBAL_GSON.toJson(mOtherServerConfig, Servers::class.java)
                )
                refreshOtherServer()
            }.showDialog()
    }

    private fun refreshActionPopupWindow(anchorView: View, binding: ViewBinding) {
        mServerActionPopupWindow.apply {
            binding.root.measure(0, 0)
            this.contentView = binding.root
            this.width = binding.root.measuredWidth
            this.height = binding.root.measuredHeight
            showAsDropDown(anchorView)
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun event(event: AccountUpdateEvent) {
        refreshSkinPane()
        reloadRecyclerView()
    }

    override fun onClick(v: View) {
        val activity = requireActivity()
        binding.apply {
            when (v) {
                returnButton -> ZHTools.onBackPressed(activity)
                addServer -> {
                    refreshActionPopupWindow(v, ViewAddOtherServerBinding.inflate(LayoutInflater.from(activity)).apply {
                        val onClickListener = View.OnClickListener { v1 ->
                            when(v1) {
                                addOtherServer -> showServerTypeSelectDialog(R.string.other_login_yggdrasil_api, 0)
                                addUniformPass -> showServerTypeSelectDialog(R.string.other_login_32_bit_server, 1)
                            }
                            mServerActionPopupWindow.dismiss()
                        }
                        addOtherServer.setOnClickListener(onClickListener)
                        addUniformPass.setOnClickListener(onClickListener)
                    })
                }
                else -> {}
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operationLayout, Animations.BounceInLeft))
                .apply(AnimPlayer.Entry(accountTypeLayout, Animations.BounceInDown))
                .apply(AnimPlayer.Entry(accountsRecycler, Animations.BounceInUp))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(operationLayout, Animations.FadeOutRight))
                .apply(AnimPlayer.Entry(accountTypeLayout, Animations.FadeOutUp))
                .apply(AnimPlayer.Entry(accountsRecycler, Animations.FadeOutDown))
        }
    }
}