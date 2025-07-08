package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class PointService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable
) {

    fun getPoint(userId: Long): UserPoint {
        return userPointTable.selectById(userId)
    }

    fun charge(userId: Long, chargeAmount: Long): UserPoint {
        TODO("구현 예정")
    }

    fun getHistories(userId: Long): List<PointHistory> {
        TODO("구현 예정")
    }
}
