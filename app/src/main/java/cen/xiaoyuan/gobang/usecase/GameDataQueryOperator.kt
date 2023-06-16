package cen.xiaoyuan.gobang.usecase

import cen.xiaoyuan.gobang.data.GameDao
import cen.xiaoyuan.gobang.data.GoBangGame
import javax.inject.Inject

class GameDataQueryOperator @Inject constructor(private val gameDao: GameDao) {
    operator fun invoke() = gameDao.queryGames()
}