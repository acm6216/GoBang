package cen.xiaoyuan.gobang.preference

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import cen.xiaoyuan.gobang.R
import com.google.android.material.button.MaterialButtonToggleGroup

abstract class TogglePreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Preference(context, attrs, defStyleAttr, defStyleRes) {

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(R.id.icon) as ImageView).setImageDrawable(icon)
        (holder.findViewById(R.id.title) as TextView).text = title
        (holder.findViewById(R.id.summary_text) as TextView).text = summary
        (holder.findViewById(R.id.toggle_group) as MaterialButtonToggleGroup).bind()
    }

    abstract fun MaterialButtonToggleGroup.bind()
}