package com.gymcore.invoice;

import com.gymcore.invoice.service.BillingSchedule;
import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BillingScheduleTest {
    @Test
    void adjustsShortMonthsAndRestoresOriginalDay() {
        assertThat(BillingSchedule.dueDates(LocalDate.of(2025, 1, 31), null, LocalDate.of(2025, 3, 31)).toList())
            .containsExactly(LocalDate.of(2025, 1, 31), LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 31));
    }
    @Test
    void supportsLeapYearsAndInclusiveEnd() {
        assertThat(BillingSchedule.dueDates(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 4, 30)).toList())
            .containsExactly(LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29));
    }
    @Test
    void neverAnticipatesDueDates() {
        assertThat(BillingSchedule.dueDates(LocalDate.of(2026, 1, 31), null, LocalDate.of(2026, 2, 27)).toList())
            .containsExactly(LocalDate.of(2026, 1, 31));
        assertThat(BillingSchedule.dueDates(LocalDate.of(2026, 3, 1), null, LocalDate.of(2026, 2, 28))).isEmpty();
    }
    @Test
    void rejectsOverlappingInclusiveBoundaries() {
        assertThat(BillingSchedule.overlaps(LocalDate.of(2026, 2, 1), null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))).isTrue();
        assertThat(BillingSchedule.overlaps(LocalDate.of(2026, 2, 2), null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1))).isFalse();
    }
}
