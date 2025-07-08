package io.hhplus.tdd.point

enum class ErrorCode(val message: String) {
    INVALID_USE_AMOUNT("포인트는 1원 이상부터 사용 가능합니다."),
    INSUFFICIENT_BALANCE("잔고가 부족합니다.")
}