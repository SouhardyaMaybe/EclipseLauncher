package me.shadow.eclipselauncher.feature.download

import me.shadow.eclipselauncher.feature.download.InfoAdapter.createCategoryView
import me.shadow.eclipselauncher.feature.download.InfoAdapter.getTagTextView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView

import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.flexbox.FlexboxLayout
import me.shadow.eclipselauncher.R
import me.shadow.eclipselauncher.databinding.ItemModDependenciesBinding
import me.shadow.eclipselauncher.event.value.AddFragmentEvent
import me.shadow.eclipselauncher.feature.download.enums.Category
import me.shadow.eclipselauncher.feature.download.enums.ModLoader
import me.shadow.eclipselauncher.feature.download.enums.Platform
import me.shadow.eclipselauncher.feature.download.item.DependenciesInfoItem
import me.shadow.eclipselauncher.feature.download.item.InfoItem
import me.shadow.eclipselauncher.feature.download.utils.DependencyUtils
import me.shadow.eclipselauncher.setting.AllSettings
import me.shadow.eclipselauncher.ui.fragment.DownloadModFragment
import me.shadow.eclipselauncher.utils.NumberWithUnits

import me.shadow.eclipselauncher.core.Tools

import org.greenrobot.eventbus.EventBus

import java.util.StringJoiner
import java.util.concurrent.Future

class ModDependenciesAdapter(
    private val mInfoItem: InfoItem,
    private val mData: List<DependenciesInfoItem>
) : RecyclerView.Adapter<ModDependenciesAdapter.InnerHolder>() {
    private var onClickListener: SetOnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder {
        return InnerHolder(ItemModDependenciesBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.setData(mData[position])
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    fun setOnItemCLickListener(listener: SetOnClickListener) {
        this.onClickListener = listener
    }

    fun interface SetOnClickListener {
        fun onItemClick()
    }

    inner class InnerHolder(private val binding: ItemModDependenciesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context: Context = binding.root.context
        private var mExtensionFuture: Future<*>? = null

        @SuppressLint("CheckResult")
        fun setData(infoItem: DependenciesInfoItem) {
            val extensionFuture = mExtensionFuture
            if (extensionFuture != null) {
                extensionFuture.cancel(true)
                mExtensionFuture = null
            }

            binding.root.background.setTint(infoItem.dependencyType.color)

            binding.sourceImageview.setImageDrawable(getPlatformIcon(infoItem.platform))
            binding.sourceTextview.text = infoItem.platform.pName

            binding.titleTextview.text = infoItem.title

            binding.categoriesLayout.removeAllViews()
            binding.tagsLayout.removeAllViews()
            for (category in infoItem.category) {
                addCategoryView(binding.categoriesLayout, context.getString(category.resNameID))
            }

            binding.bodyTextview.text = infoItem.description

            binding.tagsLayout.addView(
                getTagTextView(
                    context,
                    R.string.download_info_dependencies,
                    DependencyUtils.getTextFromType(context, infoItem.dependencyType)
                )
            )

            binding.tagsLayout.addView(
                getTagTextView(
                    context,
                    R.string.download_info_downloads,
                    NumberWithUnits.formatNumberWithUnit(infoItem.downloadCount)
                )
            )

            val modloaderSJ = StringJoiner(", ")
            for (modloader in infoItem.modloaders) {
                modloaderSJ.add(modloader.loaderName)
            }
            val modloaderText: String
            if (modloaderSJ.length() > 0) modloaderText = modloaderSJ.toString()
            else modloaderText = context.getString(R.string.generic_unknown)
            binding.tagsLayout.addView(
                getTagTextView(context, R.string.download_info_modloader, modloaderText)
            )

            binding.thumbnailImageview.setImageDrawable(null)
            val builder: RequestBuilder<Drawable> = Glide.with(context).load(infoItem.iconUrl)
            if (!AllSettings.resourceImageCache.getValue()) builder.diskCacheStrategy(DiskCacheStrategy.NONE)
            builder.into(binding.thumbnailImageview)

            itemView.setOnClickListener {
                EventBus.getDefault().post(AddFragmentEvent(
                    DownloadModFragment::class.java, DownloadModFragment.TAG, null
                ) { fragmentActivity ->
                    val viewModel = ViewModelProvider(fragmentActivity).get(InfoViewModel::class.java)
                    viewModel.infoItem = infoItem
                    viewModel.platformHelper = mInfoItem.platform.helper
                })

                onClickListener?.onItemClick()
            }
        }

        private fun getPlatformIcon(platform: Platform): Drawable? {
            if (platform == Platform.MODRINTH) return ContextCompat.getDrawable(context, R.drawable.ic_modrinth)
            if (platform == Platform.CURSEFORGE) return ContextCompat.getDrawable(context, R.drawable.ic_curseforge)
            return null
        }

        private fun addCategoryView(layout: FlexboxLayout, text: String) {
            val textView = createCategoryView(context)
            textView.text = text
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, Tools.dpToPx(9F))
            layout.addView(textView)
        }
    }
}
