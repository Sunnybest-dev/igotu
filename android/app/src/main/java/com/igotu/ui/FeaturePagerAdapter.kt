package com.igotu.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.igotu.databinding.ItemFeatureBinding

class FeaturePagerAdapter(
    private val items: List<FeatureItem>
) : RecyclerView.Adapter<FeaturePagerAdapter.VH>() {

    inner class VH(val binding: ItemFeatureBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemFeatureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.ivFeatureIcon.setImageResource(item.iconRes)
        holder.binding.tvFeatureTitle.setText(item.titleRes)
        holder.binding.tvFeatureDesc.setText(item.descRes)
    }
}
