package io.hhplus.tdd.point

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.hhplus.tdd.point.exception.ErrorCode
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PointController::class)
class PointControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var pointService: PointService

    companion object {
        private const val TEST_USER_ID = 1L
        private const val NON_EXISTENT_USER_ID = 999L
        private const val VALID_CHARGE_AMOUNT = 10000L
        private const val VALID_USE_AMOUNT = 3000L
        private const val INVALID_NEGATIVE_AMOUNT = -1000L
        private const val ZERO_AMOUNT = 0L
        private const val EXCEED_MAX_AMOUNT = 100000L
        private const val CURRENT_POINT = 5000L
        private const val UPDATED_POINT_AFTER_CHARGE = 15000L
        private const val UPDATED_POINT_AFTER_USE = 7000L
        private const val HISTORY_CHARGE_AMOUNT = 10000L
        private const val HISTORY_USE_AMOUNT = 3000L

        private const val HTTP_BAD_REQUEST_CODE = "400"
    }

    @Test
    fun `특정 유저의 포인트 조회 - 성공`() {
        // given
        val userId = TEST_USER_ID
        val userPoint = UserPoint(
            id = userId,
            point = CURRENT_POINT,
            updateMillis = System.currentTimeMillis()
        )
        every { pointService.getPoint(userId) } returns userPoint

        // when & then
        mockMvc.perform(get("/point/{id}", userId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.point").value(CURRENT_POINT))
            .andExpect(jsonPath("$.updateMillis").exists())

        verify { pointService.getPoint(userId) }
    }

    @Test
    fun `특정 유저의 포인트 충전 이용 내역 조회 - 성공`() {
        // given
        val userId = TEST_USER_ID
        val histories = listOf(
            PointHistory(
                id = 1L,
                userId = userId,
                type = TransactionType.CHARGE,
                amount = HISTORY_CHARGE_AMOUNT,
                timeMillis = System.currentTimeMillis()
            ),
            PointHistory(
                id = 2L,
                userId = userId,
                type = TransactionType.USE,
                amount = HISTORY_USE_AMOUNT,
                timeMillis = System.currentTimeMillis()
            )
        )
        every { pointService.getHistories(userId) } returns histories

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].userId").value(userId))
            .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.toString()))
            .andExpect(jsonPath("$[0].amount").value(HISTORY_CHARGE_AMOUNT))
            .andExpect(jsonPath("$[1].userId").value(userId))
            .andExpect(jsonPath("$[1].type").value(TransactionType.USE.toString()))
            .andExpect(jsonPath("$[1].amount").value(HISTORY_USE_AMOUNT))

        verify { pointService.getHistories(userId) }
    }

    @Test
    fun `특정 유저의 포인트 충전 - 성공`() {
        // given
        val userId = TEST_USER_ID
        val chargeAmount = VALID_CHARGE_AMOUNT
        val resultUserPoint = UserPoint(
            id = userId,
            point = UPDATED_POINT_AFTER_CHARGE,
            updateMillis = System.currentTimeMillis()
        )
        every { pointService.charge(userId, chargeAmount) } returns resultUserPoint

        // when & then
        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(chargeAmount.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.point").value(UPDATED_POINT_AFTER_CHARGE))
            .andExpect(jsonPath("$.updateMillis").exists())

        verify { pointService.charge(userId, chargeAmount) }
    }

    @Test
    fun `특정 유저의 포인트 사용 - 성공`() {
        // given
        val userId = TEST_USER_ID
        val useAmount = VALID_USE_AMOUNT
        val resultUserPoint = UserPoint(
            id = userId,
            point = UPDATED_POINT_AFTER_USE,
            updateMillis = System.currentTimeMillis()
        )
        every { pointService.use(userId, useAmount) } returns resultUserPoint

        // when & then
        mockMvc.perform(
            patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(useAmount.toString())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(userId))
            .andExpect(jsonPath("$.point").value(UPDATED_POINT_AFTER_USE))
            .andExpect(jsonPath("$.updateMillis").exists())

        verify { pointService.use(userId, useAmount) }
    }

    @Test
    fun `포인트 충전 - 잘못된 금액으로 예외 발생`() {
        // given
        val userId = TEST_USER_ID
        val invalidAmount = INVALID_NEGATIVE_AMOUNT
        every {
            pointService.charge(
                userId,
                invalidAmount
            )
        } throws IllegalArgumentException(ErrorCode.INVALID_CHARGE_AMOUNT.message)

        // when & then
        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidAmount.toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(HTTP_BAD_REQUEST_CODE))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CHARGE_AMOUNT.message))

        verify { pointService.charge(userId, invalidAmount) }
    }

    @Test
    fun `포인트 사용 - 잔고 부족으로 예외 발생`() {
        // given
        val userId = TEST_USER_ID
        val useAmount = VALID_CHARGE_AMOUNT
        every {
            pointService.use(
                userId,
                useAmount
            )
        } throws IllegalArgumentException(ErrorCode.INSUFFICIENT_BALANCE.message)

        // when & then
        mockMvc.perform(
            patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(useAmount.toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(HTTP_BAD_REQUEST_CODE))
            .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_BALANCE.message))

        verify { pointService.use(userId, useAmount) }
    }

    @Test
    fun `포인트 조회 - 존재하지 않는 사용자 ID로 조회시 0포인트 반환`() {
        // given
        val nonExistentUserId = NON_EXISTENT_USER_ID
        val defaultUserPoint = UserPoint(
            id = nonExistentUserId,
            point = 0L,
            updateMillis = System.currentTimeMillis()
        )
        every { pointService.getPoint(nonExistentUserId) } returns defaultUserPoint

        // when & then
        mockMvc.perform(get("/point/{id}", nonExistentUserId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(nonExistentUserId))
            .andExpect(jsonPath("$.point").value(0))
            .andExpect(jsonPath("$.updateMillis").exists())

        verify { pointService.getPoint(nonExistentUserId) }
    }

    @Test
    fun `포인트 내역 조회 - 내역이 없는 사용자는 빈 리스트 반환`() {
        // given
        val userId = TEST_USER_ID
        every { pointService.getHistories(userId) } returns emptyList()

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))

        verify { pointService.getHistories(userId) }
    }

    @Test
    fun `포인트 충전 - 최대 잔고 초과로 예외 발생`() {
        // given
        val userId = TEST_USER_ID
        val chargeAmount = EXCEED_MAX_AMOUNT
        every {
            pointService.charge(
                userId,
                chargeAmount
            )
        } throws IllegalArgumentException(ErrorCode.EXCEED_MAX_BALANCE.message)

        // when & then
        mockMvc.perform(
            patch("/point/{id}/charge", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(chargeAmount.toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(HTTP_BAD_REQUEST_CODE))
            .andExpect(jsonPath("$.message").value(ErrorCode.EXCEED_MAX_BALANCE.message))

        verify { pointService.charge(userId, chargeAmount) }
    }

    @Test
    fun `포인트 사용 - 0원 사용 시 예외 발생`() {
        // given
        val userId = TEST_USER_ID
        val zeroAmount = ZERO_AMOUNT
        every {
            pointService.use(
                userId,
                zeroAmount
            )
        } throws IllegalArgumentException(ErrorCode.INVALID_USE_AMOUNT.message)

        // when & then
        mockMvc.perform(
            patch("/point/{id}/use", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(zeroAmount.toString())
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(HTTP_BAD_REQUEST_CODE))
            .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_USE_AMOUNT.message))

        verify { pointService.use(userId, zeroAmount) }
    }

    @Test
    fun `잘못된 HTTP 메서드로 요청 시 405 에러 발생`() {
        // given
        val userId = TEST_USER_ID

        // when & then
        mockMvc.perform(post("/point/{id}", userId))
            .andExpect(status().isMethodNotAllowed)
    }

    @Test
    fun `잘못된 URL 경로로 요청 시 404 에러 발생`() {
        // when & then
        mockMvc.perform(get("/point/invalid/path"))
            .andExpect(status().isNotFound)
    }
}
