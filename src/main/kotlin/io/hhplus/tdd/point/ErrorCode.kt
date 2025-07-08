package io.hhplus.tdd.point

enum class ErrorCode(val message: String) {
    INVALID_CHARGE_AMOUNT("충전 금액은 1원 이상이어야 합니다"),
    EXCEED_MAX_BALANCE("최대 잔고를 초과할 수 없습니다"),

    INVALID_USE_AMOUNT("포인트는 1원 이상부터 사용 가능합니다."),
    INSUFFICIENT_BALANCE("잔고가 부족합니다.")
}