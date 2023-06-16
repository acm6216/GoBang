package cen.xiaoyuan.gobang.activity

import androidx.lifecycle.ViewModel
import cen.xiaoyuan.gobang.usecase.GameDataQueryOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class MainViewModel :ViewModel() {

    private val _undo = Channel<Unit>(capacity = Channel.CONFLATED)
    val undo = _undo.receiveAsFlow()
    fun undo() = _undo.trySend(Unit)

    private val _save = Channel<Unit>(capacity = Channel.CONFLATED)
    val save = _save.receiveAsFlow()
    fun save() = _save.trySend(Unit)

    private val _new = Channel<Unit>(capacity = Channel.CONFLATED)
    val new = _new.receiveAsFlow()
    fun new() = _new.trySend(Unit)

    private val _toast = Channel<Int>(capacity = Channel.CONFLATED)
    val toast = _toast.receiveAsFlow()
    fun toast(id:Int){
        _toast.trySend(id)
    }

}