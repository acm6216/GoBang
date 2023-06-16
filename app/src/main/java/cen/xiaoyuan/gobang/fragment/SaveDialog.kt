package cen.xiaoyuan.gobang.fragment

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.adapters.GameAdapter
import cen.xiaoyuan.gobang.databinding.FragmentSaveBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SaveDialog : BaseDialog<FragmentSaveBinding>() {

    override fun MaterialAlertDialogBuilder.init(): MaterialAlertDialogBuilder {
        setTitle(R.string.action_save_load)
        setPositiveButton(R.string.close,null)
        return this
    }

    override fun scrollView(): View = binding.recycler

    override fun setLayout(): FragmentSaveBinding = FragmentSaveBinding.inflate(layoutInflater,null,false)
    override val isCreateView: Boolean get() = true

    private val game: GameDataViewModel by activityViewModels()

    @Inject
    lateinit var gson: Gson

    private lateinit var gameAdapter:GameAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (binding.recycler.layoutManager as GridLayoutManager).apply {
            spanCount = if(isLandScape()) 4 else 3
        }
        binding.recycler.adapter = GameAdapter(gson) { id,go ->
            when(id){
                R.id.menu_save_load -> {
                    game.load(go)
                    dismissNow()
                }
                R.id.menu_save_delete -> game.delete(go)
                else -> {}
            }
        }.apply { gameAdapter = this }
        binding.recycler.addItemDecoration(GridSpacingItemDecoration(3, 4.dp.toInt()))

        repeatWithViewLifecycle {
            launch {
                game.items.collect { items ->
                    gameAdapter.submitList(items)
                }
            }
        }

    }

    private fun isLandScape():Boolean{
        val mConfiguration = resources.configuration
        val ori = mConfiguration.orientation
        return ori == Configuration.ORIENTATION_LANDSCAPE
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int = 1,
        private val spacing: Int = 0
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position: Int = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        }
    }

}