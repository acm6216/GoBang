package cen.xiaoyuan.gobang.view

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.adapters.NavigationAdapter

class RailView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    var listener: ((Int)->Unit)? = null
    var isHorizontal = true
    var isAuto = false
    private var unit:((item: NavigationAdapter.Items)->Unit)? = null

    private lateinit var _adapter: NavigationAdapter

    init {
        createMenu(attrs)
    }

    private fun createMenu(attrs: AttributeSet?){
        if(attrs==null || visibility != View.VISIBLE) return
        val typedArray = context.obtainStyledAttributes(attrs,R.styleable.RailView)
        if(typedArray.hasValue(R.styleable.RailView_nav_menu)) {
            val menuId = typedArray.getResourceId(R.styleable.RailView_nav_menu, -1)
            isAuto = typedArray.getBoolean(R.styleable.RailView_nav_auto,false)
            isHorizontal = if(isAuto) isLandScape() else typedArray.getBoolean(R.styleable.RailView_nav_is_horizontal,true)
            layoutManager = LinearLayoutManager(context,if(isHorizontal) LinearLayoutManager.HORIZONTAL else LinearLayoutManager.VERTICAL,false)
            _adapter = NavigationAdapter(menuId, context) {
                listener?.invoke(it.id)
                unit?.invoke(it)
            }
            adapter = _adapter
        }
        typedArray.recycle()
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    private fun isLandScape():Boolean{
        val mConfiguration = resources.configuration
        val ori = mConfiguration.orientation
        return ori == Configuration.ORIENTATION_LANDSCAPE
    }

}