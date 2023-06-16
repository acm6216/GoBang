package cen.xiaoyuan.gobang.preference

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButtonToggleGroup

class ChessBoardPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : TogglePreference(context, attrs, defStyleAttr, defStyleRes) {

    override fun MaterialButtonToggleGroup.bind(){
        ChessBoardCreator(this@ChessBoardPreference,this).execute()
    }

}