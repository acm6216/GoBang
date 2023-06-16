package cen.xiaoyuan.gobang.adapters

import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cen.xiaoyuan.gobang.R
import cen.xiaoyuan.gobang.data.GoBangGame
import cen.xiaoyuan.gobang.databinding.GameItemBinding
import cen.xiaoyuan.gobang.layoutInflater
import com.google.gson.Gson
import javax.inject.Inject

class GameAdapter(
    private val gson: Gson,
    private val listener:((Int,GoBangGame)->Unit)?=null
) : ListAdapter<GoBangGame, GameAdapter.GameViewHolder>(
    GameDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder
        = GameViewHolder.from(parent)

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(getItem(position),gson,listener)
    }

    override fun getItem(position: Int): GoBangGame {
        return super.getItem(currentList.size-1-position)
    }

    class GameViewHolder private constructor(
        val binding: GameItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(game: GoBangGame,gson:Gson, listener:((Int,GoBangGame)->Unit)?=null) {
            binding.game = game
            binding.gson = gson
            binding.root.setOnClickListener {
                saveOperator(game,it,listener)
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) = GameViewHolder(
                GameItemBinding.inflate(parent.context.layoutInflater, parent, false)
            )
        }

        private fun saveOperator(game: GoBangGame,view: View,listener:((Int,GoBangGame)->Unit)?=null){
            PopupMenu(view.context,view).apply {
                inflate(R.menu.save_menu)
                setOnMenuItemClickListener {
                    listener?.invoke(it.itemId,game)
                    true
                }
                show()
            }
        }
    }

    class GameDiffCallback : DiffUtil.ItemCallback<GoBangGame>() {

        override fun areItemsTheSame(oldItem: GoBangGame, newItem: GoBangGame): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GoBangGame, newItem: GoBangGame): Boolean {
            return oldItem == newItem
        }
    }

}
