package com.gymcore.invoice.service;

import java.time.*;
import java.util.stream.Stream;

public final class BillingSchedule {
    private BillingSchedule() {}

    public static LocalDate dueDate(LocalDate start, YearMonth month) {
        return month.atDay(Math.min(start.getDayOfMonth(), month.lengthOfMonth()));
    }
    // End dates are inclusive. Month arithmetic always uses the original billing
    // day; a February adjustment must not drift March's due date.
    public static Stream<LocalDate> dueDates(LocalDate start, LocalDate end, LocalDate through) {
        LocalDate limit = end != null && end.isBefore(through) ? end : through;
        if (limit.isBefore(start)) return Stream.empty();
        return Stream.iterate(YearMonth.from(start), month -> month.plusMonths(1))
                .limit(java.time.temporal.ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(limit)) + 1)
                .map(month -> dueDate(start, month)).filter(due -> !due.isAfter(limit));
    }
    public static boolean overlaps(LocalDate start, LocalDate end, LocalDate otherStart, LocalDate otherEnd) {
        return (otherEnd == null || !start.isAfter(otherEnd)) && (end == null || !otherStart.isAfter(end));
    }
}
