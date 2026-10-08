package me.shadow.eclipselauncher.ui.subassembly.account

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.ViewAccountBinding
import me.shadow.eclipselauncher.feature.accounts.AccountUtils
import me.shadow.eclipselauncher.feature.accounts.AccountsManager
import me.shadow.eclipselauncher.feature.log.Logging
import me.shadow.eclipselauncher.ui.fragment.AccountFragment
import me.shadow.eclipselauncher.ui.fragment.FragmentWithAnim
import me.shadow.eclipselauncher.utils.ZHTools
import me.shadow.eclipselauncher.utils.skin.SkinLoader
import me.shadow.eclipselauncher.pojav.Tools

class AccountViewWrapper(private val parentFragment: FragmentWithAnim? = null, val binding: ViewAccountBinding) {
    private val mContext: Context = binding.root.context

    init {
        parentFragment?.let { fragment ->
            binding.root.setOnClickListener {
                ZHTools.swapFragmentWithAnim(fragment, AccountFragment::class.java, AccountFragment.TAG, null)
            }
        }
    }

    fun refreshAccountInfo() {
        binding.apply {
            val account = AccountsManager.currentAccount
            account ?: run {
                if (parentFragment == null) {
                    userIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_help))
                    userName.text = null
                } else {
                    userIcon.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_add))
                    userName.setText(R.string.account_add)
                }
                accountType.visibility = View.GONE
                return
            }

            runCatching {
                userIcon.setImageDrawable(
                    SkinLoader.getAvatarDrawable(
                        mContext,
                        account,
                        Tools.dpToPx(
                            mContext.resources.getDimensionPixelSize(R.dimen._52sdp).toFloat()
                        ).toInt()
                    )
                )
            }.onFailure { e ->
                Logging.e("AccountViewWrapper", "Failed to load avatar.", e)
            }

            userName.text = account.username
            accountType.text = AccountUtils.getAccountTypeName(mContext, account)
            accountType.visibility = View.VISIBLE
        }
    }
}
