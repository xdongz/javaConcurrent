/*
 * Copyright 2021 Synopsys Inc. All rights reserved.
 * This file is confidential material. Unauthorized distribution prohibited.
 */
package chapter08.schedule_pool;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SchedulePool {

  //如何让每周四18:00:00定时执行任务
  public static void main(String[] args) {
    //获取当前时间
    LocalDateTime now = LocalDateTime.now();

    //获取周四的时间
    LocalDateTime time = now.withHour(18).withMinute(0).withSecond(0).withNano(0).with(DayOfWeek.THURSDAY);

    //如果当前时间已经过了周四，那么要获取下周四的时间，上一步只能获取本周四的时间
    if (now.compareTo(time) > 0) {
      time = time.plusWeeks(1);
    }

    //initialDealy 代表当前时间和周四的时间差
    //period 一周的时间间隔
    long initialDelay = Duration.between(now, time).toMillis();
    long period = 1000 * 60 * 60 * 24 * 7;
    ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
    pool.scheduleAtFixedRate(() -> {
      System.out.println("running");
    }, initialDelay, period, TimeUnit.MILLISECONDS);
  }
}
