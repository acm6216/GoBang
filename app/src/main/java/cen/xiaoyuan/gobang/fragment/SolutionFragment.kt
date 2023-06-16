package cen.xiaoyuan.gobang.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import cen.xiaoyuan.gobang.*
import cen.xiaoyuan.gobang.SpManager.putString
import cen.xiaoyuan.gobang.activity.MainViewModel
import cen.xiaoyuan.gobang.data.Chess
import cen.xiaoyuan.gobang.data.GameOver
import cen.xiaoyuan.gobang.data.GoBangGame
import cen.xiaoyuan.gobang.databinding.FragmentSolutionBinding
import cen.xiaoyuan.gobang.drawable.Meteor
import cen.xiaoyuan.gobang.player.AI2
import cen.xiaoyuan.gobang.drawable.StarrySky
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SolutionFragment : BaseFragment<FragmentSolutionBinding>() {

    override fun setLayout(): FragmentSolutionBinding = FragmentSolutionBinding.inflate(layoutInflater)
    private val main: MainViewModel by activityViewModels()
    private val game: GameDataViewModel by activityViewModels()
    private val goBang = GoBang.instance
    private lateinit var ai:AI2

    @Inject
    lateinit var gson: Gson

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        goBang.bind(goBangView)

        ai = AI2(goBang).apply {
            doNext {
                progress.fadeToVisibilityUnsafe(false)
                launch(Dispatchers.Main) {
                    goBangView.isEnable(true)
                    goBangView.play(it)
                    goBang.play(it)
                }
            }
        }

        goBangView.listener { chess ->
            goBang.play(chess)
        }

        goBang.apply {
            clearStatus()
            gameOverListener(
                gameOverBlock = {
                    if(it==null) main.toast(R.string.solution_game_over_none)
                    else goBangView.gameOver(it)
                },
                winnerBlock = { winner, chess ->
                    goBangView.isEnable(false)
                    if(winner)
                    main.toast(
                        if(!chess.isWhite) R.string.solution_game_over_human
                        else R.string.solution_game_over_ai
                    )
                }
            )
            gameDoNextListener {
                if (it) {
                    goBangView.isEnable(false)
                    progress.fadeToVisibilityUnsafe(true)
                    ai.next()
                } else goBangView.isEnable(true)
            }
        }

        image.treeObserver {
            starrySky()
        }

        progress.fadeToVisibilityUnsafe(false)

        repeatWithViewLifecycle {
            launchCollect(main.undo) {
                if (goBang.isEmpty) main.toast(R.string.solution_undo)
                else {
                    goBangView.undo()
                    ai.finish()
                    progress.fadeToVisibilityUnsafe(false)
                }
            }
            launchCollect(main.save) { save(ai.thinking()) }
            launchCollect(game.load) {
                launch(Dispatchers.Main) {
                    getString(R.string.set_key_chess_board_size).putString("${it.boardSize} x ${it.boardSize}")
                    goBang.load(it.array()) {
                        goBangView.load()
                    }
                }
            }
            launchCollect(main.new) { newGame() }
        }

        SpManager.listen(getString(R.string.set_key_chess_sequence),javaClass.simpleName,
            SpManager.SpBoolListener(true) {
                goBangView.chessSequence = it
            })
        SpManager.listen(getString(R.string.set_key_chess_last_sequence),javaClass.simpleName,
            SpManager.SpBoolListener {
                goBangView.chessLastSequence = it
            })
        SpManager.listen(getString(R.string.set_key_magnifier),javaClass.simpleName,
            SpManager.SpBoolListener {
                goBangView.showMagnifier = it
            })
        SpManager.listen(getString(R.string.set_key_chess_board_size),javaClass.simpleName,
            SpManager.SpStringListener("15") {
                goBangView.setChessBoardByValue(it.substring(0,2).toInt())
            })
    }

    private fun newGame(){
        if(goBang.allChess.isEmpty()) {
            main.toast(R.string.solution_new_error)
            return
        }
        launch(Dispatchers.Main) {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(R.string.solution_new_title)
                setPositiveButton(R.string.solution_new_close, null)
                setNegativeButton(R.string.solution_new_ok) { _, _ ->
                    ai.finish()
                    progress.fadeToVisibilityUnsafe(false)
                    goBangView.clearStatus()
                }
                show()
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        savedInstanceState?.also { save ->
            save.run {
                this.getString(ALL_CHESS)?.let {
                    goBang.load(it.array()){
                        if(goBang.allChess.size%2==1){
                            goBangView.isEnable(false)
                            progress.fadeToVisibilityUnsafe(true)
                            ai.next()
                        }
                    }
                }
                this.getString(GAME_OVER)?.let {
                    goBangView.gameOver = gson.fromJson(it,GameOver::class.java)
                }
            }
        }
        super.onViewStateRestored(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ALL_CHESS,gson.toJson(goBang.allChess,ArrayList::class.java))
        outState.putString(GAME_OVER,gson.toJson(goBangView.gameOver,GameOver::class.java))
        super.onSaveInstanceState(outState)
    }

    private fun GoBangGame.array() = gson.fromJson(content,Array<Chess>::class.java).toMutableList()
    private fun String.array() = gson.fromJson(this,Array<Chess>::class.java).toMutableList()

    private fun save(thinking: Boolean){
        if(thinking||goBang.allChess.isEmpty()){
            main.toast(
                if(goBang.allChess.isEmpty()) R.string.solution_save_error_empty
                else R.string.solution_save_error_ai
            )
        }
        else {
            game.save(
                gson.toJson(goBang.allChess,ArrayList::class.java),
                goBang.chessBoard.size,
                goBang.allChess.size
            )
            main.toast(R.string.solution_saved)
        }
    }

    private fun starrySky(){
        val styles = resources.getStringArray(R.array.set_chess_board_bg_value)
        SpManager.listen(getString(R.string.set_key_chess_board_bg),javaClass.simpleName,SpManager.SpStringListener{ style ->
            image.setImageDrawable(when(style) {
                styles[1] -> Meteor(image.measuredWidth, image.measuredHeight).apply {
                    start()
                }
                styles[2] -> StarrySky(image.measuredWidth, image.measuredHeight).apply {
                    for (i in 0 until 300) {
                        addRandomStar()
                    }
                    setOnStarOutListener {
                        removeStar(it)
                        addRandomStar()
                    }
                    start()
                }
                else -> null
            })
        })

    }

    private val image get() = binding.starrySky
    private val goBangView get() = binding.goBang
    private val progress get() = binding.progress

    companion object{
        const val ALL_CHESS = "ALL_CHESS"
        const val GAME_OVER = "GAME_OVER"
    }

}