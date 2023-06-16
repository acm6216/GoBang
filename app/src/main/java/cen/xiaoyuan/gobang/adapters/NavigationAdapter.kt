package cen.xiaoyuan.gobang.adapters

import android.content.Context
import android.content.res.XmlResourceParser
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cen.xiaoyuan.gobang.databinding.NavigationItemBinding

class NavigationAdapter(
    menuId: Int,
    val context: Context,
    private val bind: ((item: Items) -> Unit)
) :
    RecyclerView.Adapter<NavigationAdapter.ItemHolder>() {

    private var menus: XmlResourceParser = context.resources.getLayout(menuId)
    private val items = ArrayList<Items>()

    init {
        var type = menus.eventType
        while (type != XmlResourceParser.END_DOCUMENT) {
            if (type != XmlResourceParser.START_TAG) {
                type = menus.next()
                continue
            }
            if (menus.name == "item") {
                items.add(
                    Items(
                        menus.getAttributeResourceValue(0, -1),
                        menus.getAttributeResourceValue(1, -1),
                        if (menus.attributeCount == 3) false
                        else menus.getAttributeBooleanValue(2, false),
                        menus.getAttributeResourceValue(if (menus.attributeCount == 3) 2 else 3, -1)
                    )
                )
            }
            type = menus.next()
        }
    }

    data class Items(
        val icon: Int,
        val id: Int,
        val checked: Boolean,
        val title: Int,
        var tag: String = ""
    )

    inner class ItemHolder(val binding: NavigationItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ItemHolder(
            NavigationItemBinding.inflate(
                layoutInflater,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NavigationAdapter.ItemHolder, position: Int){
        val item = items[position]
        val binding = holder.binding
        binding.navIcon.setImageResource(item.icon)
        binding.navTitle.setText(item.title)
        binding.root.setOnClickListener {
            bind.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = 1

}