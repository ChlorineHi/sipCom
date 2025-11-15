package com.sipex.server.service;

import com.sipex.common.entity.DailyStatistic;
import com.sipex.server.mapper.DailyStatisticMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyStatisticService {

    @Autowired
    private DailyStatisticMapper dailyStatisticMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CallLogService callLogService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ConferenceLogService conferenceLogService;

    @Autowired
    private ConferenceParticipantService conferenceParticipantService;

    /**
     * 生成每日统计（手动触发或定时任务）
     */
    public DailyStatistic generateTodayStatistics() {
        LocalDate today = LocalDate.now();

        DailyStatistic statistic = new DailyStatistic();
        statistic.setStatDate(today);
        statistic.setTotalUsers(userService.countAllUsers());
        statistic.setActiveUsers(conferenceParticipantService.countTodayActiveUsers());
        statistic.setTotalCalls(0); // 可以添加今日通话统计
        statistic.setTotalCallDuration(callLogService.getTodayTotalDuration().intValue());
        statistic.setTotalMessages(messageService.countTodayMessages());
        statistic.setTotalConferences(conferenceLogService.countToday());
        statistic.setTotalConferenceDuration(conferenceLogService.getTodayTotalDuration().intValue());

        dailyStatisticMapper.insertOrUpdate(statistic);
        System.out.println("✅ 今日统计已生成: " + today);

        return statistic;
    }

    /**
     * 定时任务：每天凌晨1点生成前一天的统计
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledDailyStatistics() {
        generateTodayStatistics();
    }

    /**
     * 查询指定日期统计
     */
    public DailyStatistic getByDate(LocalDate date) {
        return dailyStatisticMapper.findByDate(date);
    }

    /**
     * 按日期范围查询
     */
    public List<DailyStatistic> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return dailyStatisticMapper.findByDateRange(startDate, endDate);
    }

    /**
     * 查询最近N天统计
     */
    public List<DailyStatistic> getRecent(int days) {
        return dailyStatisticMapper.findRecent(days);
    }
}
