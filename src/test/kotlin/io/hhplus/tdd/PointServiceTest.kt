package io.hhplus.tdd

import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import io.hhplus.tdd.point.PointService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
}
