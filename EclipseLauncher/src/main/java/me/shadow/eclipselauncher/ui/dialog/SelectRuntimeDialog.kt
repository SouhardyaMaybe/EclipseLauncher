package me.shadow.eclipselauncher.ui.dialog

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.utils.runtime.RuntimeSelectedListener
import me.shadow.eclipselauncher.pojav.multirt.MultiRTUtils
import me.shadow.eclipselauncher.pojav.multirt.RTRecyclerViewAdapter
import me.shadow.eclipselauncher.pojav.multirt.Runtime

class SelectRuntimeDialog(
    context: Context,
    private val listener: RuntimeSelectedListener
) : AbstractSelectDialog(context) {

    override fun initDialog(recyclerView: RecyclerView) {
        setCancelable(false)
        setTitleText(R.string.install_select_jre_environment)
        setMessageText(R.string.install_recommend_use_jre8)

        val runtimes: MutableList<Runtime> = ArrayList(MultiRTUtils.getRuntimes())
        if (runtimes.isNotEmpty()) runtimes.add(Runtime("auto"))
        val adapter = RTRecyclerViewAdapter(runtimes, listener, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
}
