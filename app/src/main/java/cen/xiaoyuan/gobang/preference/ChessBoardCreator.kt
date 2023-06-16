package cen.xiaoyuan.gobang.preference

import android.annotation.SuppressLint
import androidx.core.view.children
import androidx.preference.Preference
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.layoutInflater
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class ChessBoardCreator(
    private val preference:Preference,
    private val toggle:MaterialButtonToggleGroup
) {

    @SuppressLint("SetTextI18n", "ResourceType")
    fun execute(){
        val ts = preference.context.resources.getStringArray(R.array.set_chess_board_size_values)
        if(toggle.childCount==0) repeat(ts.size){
            val child = preference.context.layoutInflater.inflate(R.layout.toggle_child,toggle,false) as MaterialButton
            child.id = it+1
            child.text = ts[it]
            toggle.addView(child)
        }
        preference.sharedPreferences?.let {
            val value = it.getString(preference.key,ts[0]).toString()
            toggle.children.forEachIndexed { index, i ->
                val btn = i as MaterialButton
                if(btn.text.toString().contains(value)) toggle.check(index+1)
            }
        }
        toggle.addOnButtonCheckedListener { group, checkedId, _ ->
            val text = group.findViewById<MaterialButton>(checkedId).text.toString()
            preference.sharedPreferences?.edit()?.putString(preference.key,text)?.apply()
            preference.onPreferenceChangeListener?.onPreferenceChange(preference,text)
        }
    }
}