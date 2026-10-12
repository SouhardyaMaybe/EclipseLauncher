package me.shadow.eclipselauncher.ui.subassembly.about

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.shadow.eclipselauncher.databinding.ItemAboutViewBinding
import me.shadow.eclipselauncher.utils.EclipseTools

class AboutRecyclerAdapter(private val itemBeans: List<AboutItemBean>) :
    RecyclerView.Adapter<AboutRecyclerAdapter.InnerHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder =
        InnerHolder(ItemAboutViewBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.setData(itemBeans[position])
    }

    override fun getItemCount(): Int = itemBeans.size

    class InnerHolder(private val binding: ItemAboutViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(data: AboutItemBean) {
            binding.imageView.setImageDrawable(data.icon)
            binding.titleView.text = data.title
            binding.descView.text = data.desc

            val buttonBean = data.buttonBean
            if (buttonBean != null) {
                binding.buttonView.text = buttonBean.name
                binding.buttonView.visibility = View.VISIBLE
                binding.buttonView.setOnClickListener {
                    EclipseTools.openLink(buttonBean.activity, buttonBean.url)
                }
            } else {
                binding.buttonView.visibility = View.GONE
            }
        }
    }
}
