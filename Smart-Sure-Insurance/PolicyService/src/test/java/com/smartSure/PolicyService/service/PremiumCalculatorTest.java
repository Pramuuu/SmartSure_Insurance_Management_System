package com.smartSure.PolicyService.service;

import com.smartSure.PolicyService.dto.calculation.PremiumCalculationResponse;
import com.smartSure.PolicyService.entity.Policy;
import com.smartSure.PolicyService.entity.PolicyType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for PremiumCalculator.
 *
 * PremiumCalculator is a pure computation component — no mocks needed.
 * We create a real instance and test every calculation branch.
 */
@DisplayName("PremiumCalculator Unit Tests")
class PremiumCalculatorTest {

    // Real instance — no mocks needed (no dependencies)
    private final PremiumCalculator calculator = new PremiumCalculator();

    // Shared policy type: basePremium = 500, termMonths = 12
    private PolicyType buildPolicyType(BigDecimal basePremium) {
        return PolicyType.builder()
                .id(1L)
                .name("Health Insurance")
                .basePremium(basePremium)
                .maxCoverageAmount(new BigDecimal("1000000.00"))
                .termMonths(12)
                .build();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Age factor tests
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Age factor calculation")
    class AgeFactor {

        /**
         * This tests every age bracket defined in calculateAgeFactor():
         *   <25  → 0.85
         *   <35  → 1.00
         *   <45  → 1.20
         *   <55  → 1.50
         *   <65  → 1.90
         *   ≥65  → 2.50
         *   null → 1.00
         *
         * basePremium=500, coverage=100,000 → coverageFactor=1.0
         * annual = 500 * 1.0 * ageFactor
         */
        @ParameterizedTest(name = "age={0} → annualPremium={1}")
        @MethodSource("ageBracketProvider")
        void calculateAnnual_correctAgeFactor(Integer age, String expectedAnnual) {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            BigDecimal result = calculator.calculateAnnual(
                    type, new BigDecimal("100000.00"), age);

            assertThat(result).isEqualByComparingTo(new BigDecimal(expectedAnnual));
        }

        static Stream<Arguments> ageBracketProvider() {
            return Stream.of(
                    Arguments.of(null, "500.00"),  // null age → factor 1.0
                    Arguments.of(20,   "425.00"),  // age <25  → 500 * 0.85
                    Arguments.of(30,   "500.00"),  // age <35  → 500 * 1.00
                    Arguments.of(40,   "600.00"),  // age <45  → 500 * 1.20
                    Arguments.of(50,   "750.00"),  // age <55  → 500 * 1.50
                    Arguments.of(60,   "950.00"),  // age <65  → 500 * 1.90
                    Arguments.of(70,  "1250.00")   // age ≥65  → 500 * 2.50
            );
        }

        @Test
        @DisplayName("null age should use factor 1.0 (no risk adjustment)")
        void calculateAnnual_nullAge_usesDefaultFactor() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            BigDecimal withNull = calculator.calculateAnnual(
                    type, new BigDecimal("100000.00"), null);
            BigDecimal with30 = calculator.calculateAnnual(
                    type, new BigDecimal("100000.00"), 30);

            // Both should give the same result (factor 1.0)
            assertThat(withNull).isEqualByComparingTo(with30);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Coverage factor tests
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Coverage factor calculation")
    class CoverageFactor {

        @Test
        @DisplayName("coverage = 100,000 → factor = 1.0 (base unit)")
        void coverage_100k_factorIsOne() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            BigDecimal annual = calculator.calculateAnnual(
                    type, new BigDecimal("100000.00"), 30); // age 30 → factor 1.0

            // 500 * (100000/100000) * 1.0 = 500
            assertThat(annual).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("coverage = 500,000 → factor = 5.0")
        void coverage_500k_factorIsFive() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            BigDecimal annual = calculator.calculateAnnual(
                    type, new BigDecimal("500000.00"), 30);

            // 500 * (500000/100000) * 1.0 = 500 * 5 = 2500
            assertThat(annual).isEqualByComparingTo("2500.00");
        }

        @Test
        @DisplayName("higher coverage should produce proportionally higher premium")
        void coverage_higherCoverage_higherPremium() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            BigDecimal low  = calculator.calculateAnnual(type, new BigDecimal("200000.00"), 30);
            BigDecimal high = calculator.calculateAnnual(type, new BigDecimal("500000.00"), 30);

            assertThat(high).isGreaterThan(low);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // Frequency loading tests
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Payment frequency loading")
    class FrequencyLoading {

        /**
         * For annual premium = 500, each frequency applies a loading + split:
         *   ANNUAL:      500                           = 500.00
         *   SEMI_ANNUAL: 500 * 1.01 / 2               = 252.50
         *   QUARTERLY:   500 * 1.03 / 4               = 128.75
         *   MONTHLY:     500 * 1.05 / 12              =  43.75
         *
         * basePremium=500, coverage=100k, age=30 → annual=500
         */
        @ParameterizedTest(name = "frequency={0} → perInstallment={1}")
        @MethodSource("frequencyProvider")
        void calculatePremium_correctFrequencyLoading(
                Policy.PaymentFrequency frequency, String expectedInstallment) {

            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            PremiumCalculationResponse response = calculator.calculatePremium(
                    type, new BigDecimal("100000.00"), frequency, 30);

            assertThat(response.getCalculatedPremium())
                    .isEqualByComparingTo(new BigDecimal(expectedInstallment));
        }

        static Stream<Arguments> frequencyProvider() {
            return Stream.of(
                    Arguments.of(Policy.PaymentFrequency.ANNUAL,      "500.00"),
                    Arguments.of(Policy.PaymentFrequency.SEMI_ANNUAL, "252.50"),
                    Arguments.of(Policy.PaymentFrequency.QUARTERLY,   "128.75"),
                    Arguments.of(Policy.PaymentFrequency.MONTHLY,     "43.75")
            );
        }

        @Test
        @DisplayName("ANNUAL should be the cheapest per installment (no surcharge)")
        void annual_cheapestPerInstallment() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            PremiumCalculationResponse annual = calculator.calculatePremium(
                    type, new BigDecimal("100000.00"), Policy.PaymentFrequency.ANNUAL, 30);
            PremiumCalculationResponse monthly = calculator.calculatePremium(
                    type, new BigDecimal("100000.00"), Policy.PaymentFrequency.MONTHLY, 30);

            // Annual total = 500, monthly total = 43.75 * 12 = 525 > 500
            BigDecimal monthlyTotal = monthly.getCalculatedPremium()
                    .multiply(new BigDecimal("12"));
            assertThat(monthlyTotal).isGreaterThan(annual.getCalculatedPremium());
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // calculatePremium() — full response object
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("calculatePremium() full response")
    class FullCalculation {

        @Test
        @DisplayName("should return response with all fields populated")
        void calculatePremium_returnsCompleteResponse() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));

            PremiumCalculationResponse response = calculator.calculatePremium(
                    type, new BigDecimal("100000.00"), Policy.PaymentFrequency.MONTHLY, 30);

            assertThat(response.getBasePremium())
                    .isEqualByComparingTo("500.00");
            assertThat(response.getAnnualPremium())
                    .isEqualByComparingTo("500.00");
            assertThat(response.getCalculatedPremium())
                    .isEqualByComparingTo("43.75");
            assertThat(response.getPaymentFrequency())
                    .isEqualTo("MONTHLY");
            assertThat(response.getBreakdown())
                    .contains("BasePremium=500")
                    .contains("CoverageFactor=")
                    .contains("AgeFactor=")
                    .contains("Frequency=MONTHLY");
        }

        @Test
        @DisplayName("annual premium should increase with age (risk pricing)")
        void calculatePremium_olderAge_higherPremium() {
            PolicyType type = buildPolicyType(new BigDecimal("500.00"));
            BigDecimal coverage = new BigDecimal("100000.00");

            BigDecimal young = calculator.calculateAnnual(type, coverage, 25);
            BigDecimal old   = calculator.calculateAnnual(type, coverage, 60);

            assertThat(old).isGreaterThan(young);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // installmentCount() and monthsBetweenInstallments()
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Schedule helpers")
    class ScheduleHelpers {

        @Test
        @DisplayName("installmentCount: 12-month MONTHLY → 12 installments")
        void installmentCount_monthly_12Installments() {
            assertThat(calculator.installmentCount(12, Policy.PaymentFrequency.MONTHLY))
                    .isEqualTo(12);
        }

        @Test
        @DisplayName("installmentCount: 12-month QUARTERLY → 4 installments")
        void installmentCount_quarterly_4Installments() {
            assertThat(calculator.installmentCount(12, Policy.PaymentFrequency.QUARTERLY))
                    .isEqualTo(4);
        }

        @Test
        @DisplayName("installmentCount: 12-month SEMI_ANNUAL → 2 installments")
        void installmentCount_semiAnnual_2Installments() {
            assertThat(calculator.installmentCount(12, Policy.PaymentFrequency.SEMI_ANNUAL))
                    .isEqualTo(2);
        }

        @Test
        @DisplayName("installmentCount: 12-month ANNUAL → 1 installment")
        void installmentCount_annual_1Installment() {
            assertThat(calculator.installmentCount(12, Policy.PaymentFrequency.ANNUAL))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("monthsBetweenInstallments: MONTHLY → 1 month")
        void monthsBetween_monthly_1() {
            assertThat(calculator.monthsBetweenInstallments(Policy.PaymentFrequency.MONTHLY))
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("monthsBetweenInstallments: QUARTERLY → 3 months")
        void monthsBetween_quarterly_3() {
            assertThat(calculator.monthsBetweenInstallments(Policy.PaymentFrequency.QUARTERLY))
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("monthsBetweenInstallments: ANNUAL → 12 months")
        void monthsBetween_annual_12() {
            assertThat(calculator.monthsBetweenInstallments(Policy.PaymentFrequency.ANNUAL))
                    .isEqualTo(12);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // sumActiveCoverages()
    // ════════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sumActiveCoverages()")
    class SumActiveCoverages {

        @Test
        @DisplayName("should sum coverage amounts of all provided policies")
        void sumActiveCoverages_correctTotal() {
            Policy p1 = Policy.builder().coverageAmount(new BigDecimal("500000.00")).build();
            Policy p2 = Policy.builder().coverageAmount(new BigDecimal("300000.00")).build();
            Policy p3 = Policy.builder().coverageAmount(new BigDecimal("200000.00")).build();

            BigDecimal total = calculator.sumActiveCoverages(List.of(p1, p2, p3));

            assertThat(total).isEqualByComparingTo("1000000.00");
        }

        @Test
        @DisplayName("should return ZERO for empty list")
        void sumActiveCoverages_emptyList_returnsZero() {
            BigDecimal total = calculator.sumActiveCoverages(List.of());

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}