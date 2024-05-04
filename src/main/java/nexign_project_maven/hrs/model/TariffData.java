package nexign_project_maven.hrs.model;

public record TariffData(int id, double callRateSameOperator, double callRateOtherOperator, Integer freeIncomingMinutes,
                         Double monthlyFee) {
}
