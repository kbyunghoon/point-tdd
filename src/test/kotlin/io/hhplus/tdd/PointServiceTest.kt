package io.hhplus.tdd

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.ErrorCode
import io.hhplus.tdd.point.PointService
import io.hhplus.tdd.point.TransactionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PointServiceTest {

    private lateinit var pointService: PointService
    private lateinit var userPointTable: UserPointTable
    private lateinit var pointHistoryTable: PointHistoryTable

    @BeforeEach
    fun setup() {
        userPointTable = UserPointTable()
        pointHistoryTable = PointHistoryTable()
        pointService = PointService(userPointTable, pointHistoryTable)
    }

    @Test
    fun `포인트 조회 - 존재하지 않는 사용자는 0포인트를 반환`() {
        // given
        val userId = 1L

        // when
        val result = pointService.getPoint(userId)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(0L)
        assertThat(result.updateMillis).isGreaterThan(0L)
    }

    @Test
    fun `포인트 조회 - 기존 포인트가 있는 사용자는 정확한 포인트를 반환`() {
        // given
        val userId = 2L
        val existingPoint = 1500L
        userPointTable.insertOrUpdate(userId, existingPoint)

        // when
        val result = pointService.getPoint(userId)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(existingPoint)
        assertThat(result.updateMillis).isGreaterThan(0L)
    }

    @Test
    fun `포인트 충전 - 정상적으로 충전될 경우 포인트가 증가하며 내역이 생성`() {
        // given
        val userId = 1L
        val chargeAmount = 10000L

        // when
        val result = pointService.charge(userId, chargeAmount)

        // then
        assertThat(result.id).isEqualTo(userId)
        assertThat(result.point).isEqualTo(chargeAmount)

        // 내역 검증
        var histories = pointService.getHistories(userId)
        assertThat(histories).hasSize(1)
        assertThat(histories.first().userId).isEqualTo(userId)
        assertThat(histories.first().type).isEqualTo(TransactionType.CHARGE)
        assertThat(histories.first().amount).isEqualTo(chargeAmount)
    }

    @Test
    fun `포인트 충전 - 0원 이하 충전 시 예외 발생`() {
        // given
        val userId = 1L
        val chargeAmount = 0L

        // when & then
        val exception = assertThrows<IllegalArgumentException> { pointService.charge(userId, chargeAmount) }
        assertThat(exception.message).isEqualTo(ErrorCode.INVALID_CHARGE_AMOUNT.message)
    }

    @Test
    fun `포인트 충전 - 최대 잔고 초과 충전 시 예외 발생`() {
        // given
        val userId = 1L
        val chargeAmount = 100_000_000_000L

        // when & then
        val exception = assertThrows<IllegalArgumentException> { pointService.charge(userId, chargeAmount) }
        assertThat(exception.message).isEqualTo(ErrorCode.EXCEED_MAX_BALANCE.message)
    }

    @Test
    fun `포인트 충전 - 기존 포인트에서 추가 충전할 경우 포인트가 누적되어야 함`() {
        //given
        val userId = 1L
        val firstCharge = 10000L
        val chargeAmount = 20000L
        pointService.charge(userId, firstCharge)

        // when
        val result = pointService.charge(userId, chargeAmount)

        // then
        assertThat(result.point).isEqualTo(firstCharge + chargeAmount)

        // 내역 검증
        val histories = pointService.getHistories(userId)
        assertThat(histories).hasSize(2)
        assertThat(histories[0].amount).isEqualTo(firstCharge)
        assertThat(histories[1].amount).isEqualTo(chargeAmount)
    }

    @Test
    fun `포인트 사용 - 정상적으로 사용될 경우 포인트가 감소되며 내역 생성`() {
        // given
        val userId = 1L
        val chargeAmount = 10000L
        val useAmount = 3000L
        pointService.charge(userId, chargeAmount)

        // when
        val result = pointService.use(userId, useAmount)

        // then
        assertThat(result.point).isEqualTo(chargeAmount - useAmount)

        // 내역 검증
        val histories = pointService.getHistories(userId)
        assertThat(histories).hasSize(2)
        assertThat(histories[1].type).isEqualTo(TransactionType.USE)
        assertThat(histories[1].amount).isEqualTo(useAmount)
    }

    @Test
    fun `포인트 사용 - 잔고가 부족할 경우 예외 발생`() {
        // given
        val userId = 1L
        val chargeAmount = 1000L
        val useAmount = 3000L
        pointService.charge(userId, chargeAmount)

        // when & then
        assertThrows<IllegalArgumentException> { pointService.use(userId, useAmount) }
    }

    @Test
    fun `포인트 사용 - 0원 이하 사용 시 예외 발생`() {
        // given
        val userId = 1L
        val chargeAmount = 1000L
        val useAmount = 0L
        pointService.charge(userId, chargeAmount)

        // when & then
        val exception = assertThrows<IllegalArgumentException> { pointService.use(userId, useAmount) }
        assertThat(exception.message).isEqualTo(ErrorCode.INVALID_USE_AMOUNT.message)
    }
}
