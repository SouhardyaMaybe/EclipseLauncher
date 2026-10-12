package me.shadow.eclipselauncher.core.multirt

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.dialog.SelectRuntimeDialog
import me.shadow.eclipselauncher.utils.runtime.RuntimeSelectedListener

import me.shadow.eclipselauncher.core.Architecture
import me.shadow.eclipselauncher.core.Tools

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RTRecyclerViewAdapter private constructor(
    private val mData: MutableList<Runtime>,
    private val mType: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mSelectedListener: RuntimeSelectedListener? = null
    private var mDialog: SelectRuntimeDialog? = null
    private var mIsDeleting = false

    constructor(mData: MutableList<Runtime>) : this(mData, TYPE_MODE_EDIT)

    constructor(
        mData: MutableList<Runtime>,
        listener: RuntimeSelectedListener?,
        dialog: SelectRuntimeDialog?
    ) : this(mData, TYPE_MODE_SELECT) {
        mSelectedListener = listener
        mDialog = dialog
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MODE_SELECT -> {
                val recyclableView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_select_multirt_runtime, parent, false
                )
                RTSelectViewHolder(recyclableView)
            }
            else -> {
                val recyclableView = LayoutInflater.from(parent.context).inflate(
                    R.layout.item_multirt_runtime, parent, false
                )
                RTEditViewHolder(recyclableView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_MODE_EDIT) {
            (holder as RTEditViewHolder).bindRuntime(mData[position], position)
        } else {
            (holder as RTSelectViewHolder).bindRuntime(mData[position])
        }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun isDefaultRuntime(rt: Runtime): Boolean {
        return AllSettings.defaultRuntime.getValue() == rt.name
    }

    override fun getItemViewType(position: Int): Int {
        return mType
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    fun setDefault(rt: Runtime) {
        AllSettings.defaultRuntime.put(rt.name).save()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    fun setIsEditing(isEditing: Boolean) {
        mIsDeleting = isEditing
        notifyDataSetChanged()
    }

    fun getIsEditing(): Boolean {
        return mIsDeleting
    }

    private fun getJavaVersionName(runtime: Runtime): String {
        return runtime.name.replace(".tar.xz", "")
            .replace("-", " ")
    }

    inner class RTSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mainView: View = itemView
        private val mContext: Context = itemView.context
        private val mJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version)
        private val mFullJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version_full)
        private val mProvidedByLauncherTextView: TextView = itemView.findViewById(R.id.multirt_provided_by_launcher)

        fun bindRuntime(runtime: Runtime) {
            if (runtime.name != "auto") {
                mProvidedByLauncherTextView.visibility =
                    if (runtime.isProvidedByLauncher) View.VISIBLE else View.GONE

                if (runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch!!)) {
                    mJavaVersionTextView.text = getJavaVersionName(runtime)
                    mFullJavaVersionTextView.text = runtime.versionString

                    mainView.setOnClickListener { selectRuntime(runtime.name) }
                    return
                }

                if (runtime.versionString == null) {
                    mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt)
                } else {
                    mFullJavaVersionTextView.text =
                        mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch)
                }
                mJavaVersionTextView.text = runtime.name
                mFullJavaVersionTextView.setTextColor(Color.RED)
            } else {
                //Select automatically
                mJavaVersionTextView.setText(R.string.install_auto_select)
                mFullJavaVersionTextView.visibility = View.GONE
                mainView.setOnClickListener { selectRuntime(null) }
            }
        }

        private fun selectRuntime(jreName: String?) {
            mSelectedListener?.onSelected(jreName)
            mDialog?.dismiss()
        }
    }

    inner class RTEditViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version)
        private val mFullJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version_full)
        private val mProvidedByLauncherTextView: TextView = itemView.findViewById(R.id.multirt_provided_by_launcher)
        private val mDefaultColors: ColorStateList = mFullJavaVersionTextView.textColors
        private val mSetDefaultButton: Button = itemView.findViewById(R.id.multirt_view_setdefaultbtn)
        private val mDeleteButton: ImageButton = itemView.findViewById(R.id.multirt_view_removebtn)
        private val mContext: Context = itemView.context
        var mCurrentRuntime: Runtime? = null
        var mCurrentPosition = 0

        init {
            setupOnClickListeners()
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private fun setupOnClickListeners() {
            mSetDefaultButton.setOnClickListener {
                val runtime = mCurrentRuntime
                if (runtime != null) {
                    setDefault(runtime)
                    this@RTRecyclerViewAdapter.notifyDataSetChanged()
                }
            }

            mDeleteButton.setOnClickListener {
                val runtime = mCurrentRuntime ?: return@setOnClickListener

                // Was a Task on the background pool; the IO dispatcher plays the same role,
                // and the list refresh is still posted to the UI thread through the button.
                // Tools.showError dispatches to the UI thread itself, exactly as before
                adapterScope.launch(Dispatchers.IO) {
                    try {
                        MultiRTUtils.removeRuntimeNamed(runtime.name)
                        mDeleteButton.post {
                            val bindingAdapter = bindingAdapter
                            if (bindingAdapter != null) {
                                mData.clear()
                                mData.addAll(MultiRTUtils.getRuntimes())
                                bindingAdapter.notifyDataSetChanged()
                            }
                        }
                    } catch (e: Throwable) {
                        // The Task wrapper routed every throw of the body here
                        Tools.showError(itemView.context, e)
                    }
                }
            }
        }

        fun bindRuntime(runtime: Runtime, pos: Int) {
            mCurrentRuntime = runtime
            mCurrentPosition = pos

            updateButtonsVisibility(runtime)
            mProvidedByLauncherTextView.visibility =
                if (runtime.isProvidedByLauncher) View.VISIBLE else View.GONE

            if (runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch!!)) {
                mJavaVersionTextView.text = getJavaVersionName(runtime)
                mFullJavaVersionTextView.text = runtime.versionString
                mFullJavaVersionTextView.setTextColor(mDefaultColors)

                val defaultRuntime = isDefaultRuntime(runtime)
                mSetDefaultButton.isEnabled = !defaultRuntime
                mSetDefaultButton.setText(if (defaultRuntime) R.string.generic_default else R.string.multirt_config_setdefault)
                return
            }

            // Problematic runtime moment, force propose deletion
            mDeleteButton.visibility = View.VISIBLE
            if (runtime.versionString == null) {
                mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt)
            } else {
                mFullJavaVersionTextView.text =
                    mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch)
            }
            mJavaVersionTextView.text = runtime.name
            mFullJavaVersionTextView.setTextColor(Color.RED)
            mSetDefaultButton.visibility = View.GONE
        }

        private fun updateButtonsVisibility(runtime: Runtime) {
            mSetDefaultButton.visibility = if (mIsDeleting) View.INVISIBLE else View.VISIBLE
            mDeleteButton.visibility =
                if (!mIsDeleting || runtime.isProvidedByLauncher) View.INVISIBLE else View.VISIBLE
        }
    }

    companion object {
        private const val TYPE_MODE_SELECT = 0
        private const val TYPE_MODE_EDIT = 1

        /** Background work of the view holders: runtime deletion used to run on the task pool */
        private val adapterScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("RTRecyclerViewAdapter"))
    }
}
