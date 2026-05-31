package xyz.zlov.app.strava.stats.dto;

import java.util.List;

public record MonthlyStatsByTypeDto(int year, int month, List<ActivityTypeStatsDto> activities) {}
