package io.hhplus.tdd

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointService
import io.hhplus.tdd.point.PointServiceImpl
import io.hhplus.tdd.point.TransactionType
import io.hhplus.tdd.point.exception.ErrorCode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class PointServiceBehaviorTest : BehaviorSpec({
    lateinit var pointService: PointService
    lateinit var userPointTable: UserPointTable
    lateinit var pointHistoryTable: PointHistoryTable

    beforeContainer {
        userPointTable = UserPointTable()
        pointHistoryTable = PointHistoryTable()
        pointService = PointServiceImpl(userPointTable, pointHistoryTable)
    }

    given("포인트 조회") {

        `when`("존재하지 않는 사용자의 포인트를 조회할 때") {
            val userId = 1L
            val result = pointService.getPoint(userId)

            then("사용자 ID를 가진 0포인트 정보를 반환해야 한다") {
                result.id shouldBe userId
                result.point shouldBe 0L
                result.updateMillis shouldBeGreaterThan 0L
            }
        }

        `when`("기존 포인트가 있는 사용자의 포인트를 조회할 때") {
            val userId = 2L
            val existingPoint = 1500L
            userPointTable.insertOrUpdate(userId, existingPoint)

            val result = pointService.getPoint(userId)

            then("정확한 포인트 정보를 반환해야 한다") {
                result.id shouldBe userId
                result.point shouldBe existingPoint
                result.updateMillis shouldBeGreaterThan 0L
            }
        }
    }


    given("포인트 충전") {

        `when`("정상적으로 충전이 될 때") {
            val userId = 1L
            val chargeAmount = 10000L
            val result = pointService.charge(userId, chargeAmount)

            then("사용자의 포인트는 증가해야 한다.") {
                result.id shouldBe userId
                result.point shouldBe chargeAmount
            }

            then("충전 내역이 생성되어야 한다.") {
                val histories = pointService.getHistories(userId)
                histories.size shouldBe 1
                val history = histories.first()
                history.userId shouldBe userId
                history.type shouldBe TransactionType.CHARGE
                history.amount shouldBe chargeAmount
            }
        }

        `when`("포인트 0원 이하로 충전을 시도 할 때") {
            val userId = 1L
            val chargeAmount = 0L

            then("충전 금액에 대한 예외가 발생되어야 한다.") {
                val exception = shouldThrow<IllegalArgumentException> { pointService.charge(userId, chargeAmount) }
                exception.message shouldBe ErrorCode.INVALID_CHARGE_AMOUNT.message
            }
        }

        `when`("최대 잔고 초과하는 충전을 시도할 때") {
            val userId = 1L
            val currentAmount = 900_000_000L
            val chargeAmount = 200_000_000L
            userPointTable.insertOrUpdate(userId, currentAmount)

            then("최대 잔고 초과 대한 예외가 발생되어야 한다.") {
                val exception = shouldThrow<IllegalArgumentException> { pointService.charge(userId, chargeAmount) }
                exception.message shouldBe ErrorCode.EXCEED_MAX_BALANCE.message
            }

            then("기존 잔고는 유지되어야 한다.") {
                val result = pointService.getPoint(userId)
                result.point shouldBe currentAmount
            }
        }

        `when`("최대 잔고에 정확히 달하는 충전을 시도할 때") {
            val userId = 1L
            val currentAmount = 800_000_000L
            val chargeAmount = 200_000_000L
            userPointTable.insertOrUpdate(userId, currentAmount)
            val result = pointService.charge(userId, chargeAmount)

            then("성공적으로 충전이 되어야 한다.") {
                result.point shouldBe currentAmount + chargeAmount
            }
        }

        `when`("기존 포인트에서 추가 충전할 경우") {
            val userId = 1L
            val firstCharge = 10000L
            val chargeAmount = 20000L
            pointService.charge(userId, firstCharge)
            val result = pointService.charge(userId, chargeAmount)

            then("포인트가 누적되어야 한다.") {
                result.point shouldBe firstCharge + chargeAmount
            }

            then("충전 내역이 생성되어야 한다.") {
                val histories = pointService.getHistories(userId)
                histories.size shouldBe 2
                histories.first().amount shouldBe firstCharge
                histories.last().amount shouldBe chargeAmount
            }
        }
    }

    given("포인트 사용") {

        `when`("정상적으로 포인트가 사용될 경우") {
            val userId = 1L
            val chargeAmount = 10000L
            val useAmount = 3000L
            pointService.charge(userId, chargeAmount)
            val result = pointService.use(userId, useAmount)

            then("포인트가 차감되어야 한다.") {
                result.id shouldBe userId
                result.point shouldBe chargeAmount - useAmount
            }

            then("사용 내역이 생성되어야 한다.") {
                val histories = pointService.getHistories(userId)
                histories.size shouldBe 2
                val history = histories.last()
                history.userId shouldBe userId
                history.type shouldBe TransactionType.USE
                history.amount shouldBe useAmount
            }
        }

        `when`("잔고가 부족할 경우") {
            val userId = 1L
            val chargeAmount = 1000L
            val useAmount = 3000L
            pointService.charge(userId, chargeAmount)

            then("잔고 부족에 대한 예외가 발생되어야 한다.") {
                val exception = shouldThrow<IllegalArgumentException> { pointService.use(userId, useAmount) }
                exception.message shouldBe ErrorCode.INSUFFICIENT_BALANCE.message
            }
        }

        `when`("0원 이하의 포인트를 사용할 경우") {
            val userId = 1L
            val chargeAmount = 1000L
            val useAmount = 0L
            pointService.charge(userId, chargeAmount)

            then("포인트 사용에 대한 예외가 발생되어야 한다.") {
                val exception = shouldThrow<IllegalArgumentException> { pointService.use(userId, useAmount) }
                exception.message shouldBe ErrorCode.INVALID_USE_AMOUNT.message
            }
        }
    }
})
