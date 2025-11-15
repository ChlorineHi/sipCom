package com.sipex.server.mapper;

import com.sipex.common.entity.DailyStatistic;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyStatisticMapper {

    @Insert("INSERT INTO daily_statistics (stat_date, total_users, active_users, total_calls, " +
            "total_call_duration, total_messages, total_conferences, total_conference_duration) " +
            "VALUES (#{statDate}, #{totalUsers}, #{activeUsers}, #{totalCalls}, #{totalCallDuration}, " +
            "#{totalMessages}, #{totalConferences}, #{totalConferenceDuration}) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_users = #{totalUsers}, active_users = #{activeUsers}, total_calls = #{totalCalls}, " +
            "total_call_duration = #{totalCallDuration}, total_messages = #{totalMessages}, " +
            "total_conferences = #{totalConferences}, total_conference_duration = #{totalConferenceDuration}")
    int insertOrUpdate(DailyStatistic statistic);

    @Select("SELECT * FROM daily_statistics WHERE stat_date = #{date}")
    DailyStatistic findByDate(LocalDate date);

    @Select("SELECT * FROM daily_statistics WHERE stat_date BETWEEN #{startDate} AND #{endDate} ORDER BY stat_date DESC")
    List<DailyStatistic> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT * FROM daily_statistics ORDER BY stat_date DESC LIMIT #{limit}")
    List<DailyStatistic> findRecent(@Param("limit") int limit);
}
