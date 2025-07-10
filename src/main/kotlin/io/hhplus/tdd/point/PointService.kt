package io.hhplus.tdd.point

interface PointService {
    /**
     * 유저 포인트 조회
     * @param userId 조회할 유저 ID
     * @return 유저의 포인트 정보
     */
    fun getPoint(userId: Long): UserPoint

    /**
     * 유저 포인트 충전
     * @param userId 충전할 유저 ID
     * @param chargeAmount 충전할 포인트 양
     * @return 충전 후 유저의 포인트 정보
     */
    fun charge(userId: Long, chargeAmount: Long): UserPoint

    /**
     * 유저 포인트 사용
     * @param userId 사용할 유저 ID
     * @param amount 사용할 포인트 양
     * @return 사용 후 유저의 포인트 정보
     */
    fun use(userId: Long, amount: Long): UserPoint

    /**
     * 유저 포인트 사용/충전 내역 조회
     * @param userId 조회할 유저 ID
     * @return 포인트 사용/충전 내역 리스트
     */
    fun getHistories(userId: Long): List<PointHistory>
}
