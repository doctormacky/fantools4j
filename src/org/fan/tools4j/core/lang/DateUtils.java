package org.fan.tools4j.core.lang;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**  
 * @Title: DateUtils.java
 *
 * @Description: TODO
 *
 * @author longrm
 *
 * @date 2022-08-15 02:51:18 
 */
public class DateUtils {

    public static final int MAX_TIME = 2145888000; // 2038-01-01

    public static final int SECONDS_PER_MINUTE = 60;
    public static final int SECONDS_PER_HOUR = 3600;
    public static final int SECONDS_PER_DAY = 86400;

    public static int getCurrentTime() {
        return toUnixTime(System.currentTimeMillis());
    }

    public static int getCurrentDate() {
        return getDate(getCurrentTime());
    }

    public static int getDate(int time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), 0, 0, 0);

        return toUnixTime(c.getTimeInMillis());
    }

    public static int getYear(int time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        return c.get(Calendar.YEAR);
    }

    public static int getMonth(int time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        return c.get(Calendar.MONTH);
    }

    public static int getDay(int time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        return c.get(Calendar.DAY_OF_MONTH);
    }

    public static int addMonths(int time, int amount) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        c.add(Calendar.MONTH, amount);
        return toUnixTime(c.getTimeInMillis());
    }

    public static int addDays(int time, int amount) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        c.add(Calendar.DATE, amount);
        return toUnixTime(c.getTimeInMillis());
    }

    public static String getFormatDate(int time) {
        return getFormatDate(time * 1000L, null);
    }

    public static String getFormatDate(int time, String pattern) {
        return getFormatDate(time * 1000L, pattern);
    }

    public static String getFormatDate(long millis) {
        return getFormatDate(millis, null);
    }

    public static String getFormatDate(long millis, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            pattern = "yyyy-MM-dd";
        }
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        return df.format(new Date(millis));
    }

    public static int getFirstDayTime(int time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        c.add(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        return toUnixTime(c.getTimeInMillis());
    }

    public static int getLastDayTime(int time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis((long) time * 1000);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return toUnixTime(c.getTimeInMillis());
    }

    public static int toUnixTime(long millseconds) {
        return (int) TimeUnit.MILLISECONDS.toSeconds(millseconds);
    }

    public static int toUnixTimeOfCeil(long millseconds) {
        return (int) Math.ceil(millseconds / 1000.0D);
    }

    public static int toUnixTime(Date date) {
        return toUnixTime(date.getTime());
    }

    public static int toUnixTime(String stringDate) {
        return toUnixTime(stringDate, "yyyy-MM-dd");
    }

    public static int toUnixTime(String stringDate, String pattern) {
        DateFormat sdf = new SimpleDateFormat(pattern);
        try {
            Date date = sdf.parse(stringDate);
            return toUnixTime(date.getTime());
        } catch (ParseException e) {
            throw new RuntimeException("parse date error: " + stringDate, e);
        }
    }

    public static int toUnixTime(LocalDate date) {
        String str = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return toUnixTime(str, "yyyy-MM-dd");
    }

    public static int toUnixTime(LocalDateTime dateTime) {
        String str = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return toUnixTime(str, "yyyy-MM-dd'T'HH:mm:ss");
    }

    public static LocalDate parseLocalDate(int time) {
        String dateStr = getFormatDate(time, "yyyy-MM-dd");
        return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static LocalDateTime parseLocalDateTime(int time) {
        String dateStr = getFormatDate(time, "yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public static LocalDate getNextDate(LocalDate date, int day) {
        LocalDate nextDate = LocalDate.of(date.getYear(), date.getMonth(), day);
        if (nextDate.isBefore(date)) {
            nextDate = nextDate.plusMonths(1);
        }
        return nextDate;
    }

    public static LocalDate getLastDate(LocalDate date, int day) {
        LocalDate lastDate = LocalDate.of(date.getYear(), date.getMonth(), day);
        if (lastDate.isAfter(date)) {
            lastDate = lastDate.minusMonths(1);
        }
        return lastDate;
    }

}
