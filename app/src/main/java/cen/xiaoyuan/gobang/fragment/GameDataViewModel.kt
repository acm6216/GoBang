package cen.xiaoyuan.gobang.fragment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cen.xiaoyuan.gobang.data.GoBangGame
import cen.xiaoyuan.gobang.usecase.GameDataDeleteOperator
import cen.xiaoyuan.gobang.usecase.GameDataInsertOperator
import cen.xiaoyuan.gobang.usecase.GameDataQueryOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GameDataViewModel @Inject constructor(
    gameDataQueryOperator: GameDataQueryOperator,
    private val gameDataInsertOperator: GameDataInsertOperator,
    private val gameDataDeleteOperator: GameDataDeleteOperator
):ViewModel() {

    private val games = gameDataQueryOperator()
    val items = games

    fun delete(goBangGame: GoBangGame){
        viewModelScope.launch {
            gameDataDeleteOperator.invoke(goBangGame)
        }
    }

    fun save(data:String,size:Int,progress:Int){
        viewModelScope.launch {
            val time = System.currentTimeMillis()
            gameDataInsertOperator(
                GoBangGame(
                    id = 0,
                    content = data,
                    title = "$time",
                    date = time,
                    time = 0L,
                    boardSize = size,
                    progress = progress,
                    type = 0
                )
            )
        }
    }

    private val _load = Channel<GoBangGame>(capacity = Channel.CONFLATED)
    val load = _load.receiveAsFlow()
    fun load(goBangGame: GoBangGame) = _load.trySend(goBangGame)

}