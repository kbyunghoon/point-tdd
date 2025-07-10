package io.hhplus.tdd.point

import com.ninjasquad.springmockk.MockkBean
import io.hhplus.tdd.point.exception.ErrorCode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PointController::class)
class PointControllerBehaviorTest : BehaviorSpec() {

    override fun extensions() = listOf(SpringExtension)

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var pointService: PointService

    companion object {
        private const val TEST_USER_ID = 1L
        private const val NON_EXISTENT_USER_ID = 999L
        private const val EXISTING_USER_POINT = 5000L
        private const val DEFAULT_POINT = 0L
        
        private const val VALID_CHARGE_AMOUNT = 10000L
        private const val CHARGED_RESULT_POINT = 15000L
        private const val INVALID_NEGATIVE_AMOUNT = -1000L
        private const val EXCEED_MAX_CHARGE_AMOUNT = 100000L
        
        private const val VALID_USE_AMOUNT = 3000L
        private const val USED_RESULT_POINT = 7000L
        private const val EXCEED_BALANCE_USE_AMOUNT = 10000L
        private const val ZERO_AMOUNT = 0L
        
        private const val HISTORY_ID_1 = 1L
        private const val HISTORY_ID_2 = 2L
        private const val CHARGE_HISTORY_AMOUNT = 10000L
        private const val USE_HISTORY_AMOUNT = 3000L
    }

    init {
        Given("포인트 조회 API가 호출될 때") {
            When("존재하는 사용자 ID로 요청하면") {
                val userPoint = UserPoint(
                    id = TEST_USER_ID,
                    point = EXISTING_USER_POINT,
                    updateMillis = System.currentTimeMillis()
                )
                every { pointService.getPoint(TEST_USER_ID) } returns userPoint

                Then("해당 사용자의 포인트 정보를 성공적으로 반환한다") {
                    mockMvc.perform(get("/point/{id}", TEST_USER_ID))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                        .andExpect(jsonPath("$.point").value(EXISTING_USER_POINT))
                        .andExpect(jsonPath("$.updateMillis").exists())

                    verify { pointService.getPoint(TEST_USER_ID) }
                }
            }

            When("존재하지 않는 사용자 ID로 요청하면") {
                val defaultUserPoint = UserPoint(
                    id = NON_EXISTENT_USER_ID,
                    point = DEFAULT_POINT,
                    updateMillis = System.currentTimeMillis()
                )
                every { pointService.getPoint(NON_EXISTENT_USER_ID) } returns defaultUserPoint

                Then("0포인트가 설정된 기본 사용자 정보를 반환한다") {
                    mockMvc.perform(get("/point/{id}", NON_EXISTENT_USER_ID))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(NON_EXISTENT_USER_ID))
                        .andExpect(jsonPath("$.point").value(DEFAULT_POINT))
                        .andExpect(jsonPath("$.updateMillis").exists())

                    verify { pointService.getPoint(NON_EXISTENT_USER_ID) }
                }
            }
        }

        Given("포인트 내역 조회 API가 호출될 때") {
            When("내역이 있는 사용자로 요청하면") {
                val histories = listOf(
                    PointHistory(
                        id = HISTORY_ID_1,
                        userId = TEST_USER_ID,
                        type = TransactionType.CHARGE,
                        amount = CHARGE_HISTORY_AMOUNT,
                        timeMillis = System.currentTimeMillis()
                    ),
                    PointHistory(
                        id = HISTORY_ID_2,
                        userId = TEST_USER_ID,
                        type = TransactionType.USE,
                        amount = USE_HISTORY_AMOUNT,
                        timeMillis = System.currentTimeMillis()
                    )
                )
                every { pointService.getHistories(TEST_USER_ID) } returns histories

                Then("해당 사용자의 포인트 거래 내역을 성공적으로 반환한다") {
                    mockMvc.perform(get("/point/{id}/histories", TEST_USER_ID))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$").isArray)
                        .andExpect(jsonPath("$.length()").value(2))
                        .andExpect(jsonPath("$[0].userId").value(TEST_USER_ID))
                        .andExpect(jsonPath("$[0].type").value(TransactionType.CHARGE.toString()))
                        .andExpect(jsonPath("$[0].amount").value(CHARGE_HISTORY_AMOUNT))
                        .andExpect(jsonPath("$[1].userId").value(TEST_USER_ID))
                        .andExpect(jsonPath("$[1].type").value(TransactionType.USE.toString()))
                        .andExpect(jsonPath("$[1].amount").value(USE_HISTORY_AMOUNT))

                    verify { pointService.getHistories(TEST_USER_ID) }
                }
            }

            When("내역이 없는 사용자로 요청하면") {
                every { pointService.getHistories(TEST_USER_ID) } returns emptyList()

                Then("빈 배열을 성공적으로 반환한다") {
                    mockMvc.perform(get("/point/{id}/histories", TEST_USER_ID))
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$").isArray)
                        .andExpect(jsonPath("$.length()").value(0))

                    verify { pointService.getHistories(TEST_USER_ID) }
                }
            }
        }

        Given("포인트 충전 API가 호출될 때") {
            When("올바른 충전 금액으로 요청하면") {
                val resultUserPoint = UserPoint(
                    id = TEST_USER_ID,
                    point = CHARGED_RESULT_POINT,
                    updateMillis = System.currentTimeMillis()
                )
                every { pointService.charge(TEST_USER_ID, VALID_CHARGE_AMOUNT) } returns resultUserPoint

                Then("포인트가 성공적으로 충전되고 업데이트된 정보를 반환한다") {
                    mockMvc.perform(
                        patch("/point/{id}/charge", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_CHARGE_AMOUNT.toString())
                    )
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                        .andExpect(jsonPath("$.point").value(CHARGED_RESULT_POINT))
                        .andExpect(jsonPath("$.updateMillis").exists())

                    verify { pointService.charge(TEST_USER_ID, VALID_CHARGE_AMOUNT) }
                }
            }

            When("잘못된 충전 금액(음수)으로 요청하면") {
                every { pointService.charge(TEST_USER_ID, INVALID_NEGATIVE_AMOUNT) } throws 
                    IllegalArgumentException(ErrorCode.INVALID_CHARGE_AMOUNT.message)

                Then("400 에러와 함께 에러 메시지를 반환한다") {
                    mockMvc.perform(
                        patch("/point/{id}/charge", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(INVALID_NEGATIVE_AMOUNT.toString())
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CHARGE_AMOUNT.message))

                    verify { pointService.charge(TEST_USER_ID, INVALID_NEGATIVE_AMOUNT) }
                }
            }

            When("최대 잔고를 초과하는 충전 금액으로 요청하면") {
                every { pointService.charge(TEST_USER_ID, EXCEED_MAX_CHARGE_AMOUNT) } throws 
                    IllegalArgumentException(ErrorCode.EXCEED_MAX_BALANCE.message)

                Then("400 에러와 함께 에러 메시지를 반환한다") {
                    mockMvc.perform(
                        patch("/point/{id}/charge", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(EXCEED_MAX_CHARGE_AMOUNT.toString())
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value(ErrorCode.EXCEED_MAX_BALANCE.message))

                    verify { pointService.charge(TEST_USER_ID, EXCEED_MAX_CHARGE_AMOUNT) }
                }
            }
        }

        Given("포인트 사용 API가 호출될 때") {
            When("올바른 사용 금액으로 요청하면") {
                val resultUserPoint = UserPoint(
                    id = TEST_USER_ID,
                    point = USED_RESULT_POINT,
                    updateMillis = System.currentTimeMillis()
                )
                every { pointService.use(TEST_USER_ID, VALID_USE_AMOUNT) } returns resultUserPoint

                Then("포인트가 성공적으로 사용되고 업데이트된 정보를 반환한다") {
                    mockMvc.perform(
                        patch("/point/{id}/use", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_USE_AMOUNT.toString())
                    )
                        .andExpect(status().isOk)
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id").value(TEST_USER_ID))
                        .andExpect(jsonPath("$.point").value(USED_RESULT_POINT))
                        .andExpect(jsonPath("$.updateMillis").exists())

                    verify { pointService.use(TEST_USER_ID, VALID_USE_AMOUNT) }
                }
            }

            When("잔고보다 많은 금액으로 사용 요청하면") {
                every { pointService.use(TEST_USER_ID, EXCEED_BALANCE_USE_AMOUNT) } throws 
                    IllegalArgumentException(ErrorCode.INSUFFICIENT_BALANCE.message)

                Then("400 에러와 함께 에러 메시지를 반환한다") {
                    mockMvc.perform(
                        patch("/point/{id}/use", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(EXCEED_BALANCE_USE_AMOUNT.toString())
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INSUFFICIENT_BALANCE.message))

                    verify { pointService.use(TEST_USER_ID, EXCEED_BALANCE_USE_AMOUNT) }
                }
            }

            When("0원으로 사용 요청하면") {
                every { pointService.use(TEST_USER_ID, ZERO_AMOUNT) } throws 
                    IllegalArgumentException(ErrorCode.INVALID_USE_AMOUNT.message)

                Then("400 에러와 함께 에러 메시지를 반환한다") {
                    mockMvc.perform(
                        patch("/point/{id}/use", TEST_USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(ZERO_AMOUNT.toString())
                    )
                        .andExpect(status().isBadRequest)
                        .andExpect(jsonPath("$.code").value("400"))
                        .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_USE_AMOUNT.message))

                    verify { pointService.use(TEST_USER_ID, ZERO_AMOUNT) }
                }
            }
        }

        Given("잘못된 HTTP 요청이 들어올 때") {
            When("지원하지 않는 HTTP 메서드로 요청하면") {
                Then("405 Method Not Allowed 에러를 반환한다") {
                    mockMvc.perform(post("/point/{id}", TEST_USER_ID))
                        .andExpect(status().isMethodNotAllowed)
                }
            }

            When("존재하지 않는 URL 경로로 요청하면") {
                Then("404 Not Found 에러를 반환한다") {
                    mockMvc.perform(get("/point/invalid/path"))
                        .andExpect(status().isNotFound)
                }
            }
        }

        afterSpec {
            // 모든 테스트 완료 후 mock 검증이 완료되었는지 확인
            confirmVerified(pointService)
        }
    }
}
