package io.hhplus.tdd.point

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class PointService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable
) {
    companion object {
        const val MAX_BALANCE = 1_000_000_000L
    }

    fun getPoint(userId: Long): UserPoint {
        return userPointTable.selectById(userId)
    }

    fun charge(userId: Long, chargeAmount: Long): UserPoint {
        require(chargeAmount > 0) { ErrorCode.INVALID_CHARGE_AMOUNT.message }
        require(chargeAmount < MAX_BALANCE) { ErrorCode.EXCEED_MAX_BALANCE.message }

        val currentPoint = userPointTable.selectById(userId)

        val newAmount = currentPoint.point + chargeAmount

        val updatedPoint = userPointTable.insertOrUpdate(
            id = userId,
            amount = newAmount,
        )

        pointHistoryTable.insert(
            id = userId,
            amount = chargeAmount,
            transactionType = TransactionType.CHARGE,
            updateMillis = updatedPoint.updateMillis,
        )

        return updatedPoint
    }

    fun getHistories(userId: Long): List<PointHistory> {
        return pointHistoryTable.selectAllByUserId(userId)
    }

    fun use(userId: Long, amount: Long): UserPoint {
        require(amount > 0) { ErrorCode.INVALID_USE_AMOUNT.message }

        val currentPoint = userPointTable.selectById(userId)

        val newAmount = currentPoint.point - amount

        require(newAmount >= 0) { "잔고가 부족합니다" }

        val updatedPoint = userPointTable.insertOrUpdate(
            id = userId,
            amount = newAmount
        )

        pointHistoryTable.insert(
            id = userId,
            amount = amount,
            transactionType = TransactionType.USE,
            updateMillis = updatedPoint.updateMillis,
        )

        return updatedPoint
    }
}
