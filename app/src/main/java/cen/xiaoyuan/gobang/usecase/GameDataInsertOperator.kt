package cen.xiaoyuan.gobang.usecase

import cen.xiaoyuan.gobang.data.GameDao
import cen.xiaoyuan.gobang.data.GoBangGame
import javax.inject.Inject

class GameDataInsertOperator @Inject constructor(private val gameDao: GameDao) {
    suspend operator fun invoke(game: GoBangGame) = gameDao.insertGameData(game)
}