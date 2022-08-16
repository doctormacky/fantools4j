package org.fan.tools4j.core.distributed;

import org.fan.tools4j.core.lang.DateUtils;
import org.fan.tools4j.core.lang.StringUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * ID生成器：雪花算法
 * @see <a href="https://github.com/twitter-archive/snowflake/tags">twitter-snowflake</a>
 */
public class SnowflakeIdWorker {

    /**
     * 计算精度：毫秒值（时间位41位，最多69年）
     * 秒值（时间位31位，最多68年）
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    /**
     * 最大时钟回退时间
     */
    private final long MAX_BACKWARD = 5L;
    /**
     * 开始时间截 (2015-01-01)
     */
    private final long twepoch = 1420041600000L;
    /**
     * 一天秒|毫秒值所占的位数：86400_000
     */
    private final long dayMillisBits = 27L;
    private final long daySecondsBits = 17L;
    /**
     * 机器id所占的位数
     */
    private long workerIdBits = 5L;
    /**
     * 数据中心id所占的位数
     */
    private long datacenterIdBits = 5L;
    /**
     * 序列在id中占的位数
     */
    private long sequenceBits = 12L;
    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private long maxWorkerId = -1L ^ (-1L << workerIdBits);
    /**
     * 支持的最大数据中心id，结果是31
     */
    private long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    /**
     * 机器ID向左移12位
     */
    private long workerIdShift = sequenceBits;
    /**
     * 数据中心id向左移17位(12+5)
     */
    private long datacenterIdShift = sequenceBits + workerIdBits;
    /**
     * 时间截向左移22位(5+5+12)
     */
    private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private long sequenceMask = -1L ^ (-1L << sequenceBits);
    /**
     * 按天序列ID最大值：毫秒值位数+时间偏移位
     */
    private long maxSnId = (1L << (timestampLeftShift + dayMillisBits)) - 1;

    /**
     * 工作机器ID(0~31)
     */
    private long workerId;
    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;
    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;
    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    /**
     * 构造函数
     * @param workerId     机器ID (0~31)
     * @param datacenterId 数据中心ID (0~31)
     */
    public SnowflakeIdWorker(long workerId, long datacenterId) {
        checkMachineId(workerId, datacenterId);
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    private void checkMachineId(long workerId, long datacenterId) {
        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
    }

    public synchronized SnowflakeIdWorker doSetBits(long workerIdBits, long datacenterIdBits, long sequenceBits) {
        return doSetBits(workerIdBits, datacenterIdBits, sequenceBits, TimeUnit.MILLISECONDS);
    }

    /**
     * 设置工作机器位数 & 序列位数
     * @param workerIdBits
     * @param datacenterIdBits
     * @param sequenceBits
     * @param timeUnit
     * @return
     */
    public synchronized SnowflakeIdWorker doSetBits(long workerIdBits, long datacenterIdBits, long sequenceBits, TimeUnit timeUnit) {
        // 已使用，不能重新设置
        if (lastTimestamp > 0) {
            throw new UnsupportedOperationException("Id generator is used, can't doSetBits");
        }
        if (timeUnit != TimeUnit.MILLISECONDS && timeUnit != TimeUnit.SECONDS) {
            throw new IllegalArgumentException("timeUnit is unsupported: " + timeUnit);
        }
        // 序列默认位数：毫秒12位|秒22位
        if (sequenceBits == 0) {
            sequenceBits = timeUnit == TimeUnit.MILLISECONDS ? 12L : 22L;
        }
        if (workerIdBits <= 0 || datacenterIdBits <= 0 || sequenceBits <= 0) {
            throw new IllegalArgumentException("worker Id Bits | datacenter Id Bits | sequence Bits can't be less than 0");
        }
        // 最大位数：毫秒22位（+时间41位）| 秒32位（+时间31位）
        long maxBits = timeUnit == TimeUnit.MILLISECONDS ? 22 : 32;
        if (workerIdBits + datacenterIdBits + sequenceBits > maxBits) {
            throw new IllegalArgumentException(String.format("worker Id Bits + datacenter Id Bits + sequence Bits can't be greater than %d", maxBits));
        }

        this.workerIdBits = workerIdBits;
        this.datacenterIdBits = datacenterIdBits;
        this.sequenceBits = sequenceBits;
        this.timeUnit = timeUnit;

        this.maxWorkerId = -1L ^ (-1L << workerIdBits);
        this.maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
        this.workerIdShift = sequenceBits;
        this.datacenterIdShift = sequenceBits + workerIdBits;
        this.timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
        this.sequenceMask = -1L ^ (-1L << sequenceBits);
        // 最大序列ID值：毫秒值位数+时间偏移位
        long timeBits = timeUnit == TimeUnit.MILLISECONDS ? dayMillisBits : daySecondsBits;
        this.maxSnId = (1L << (timestampLeftShift + timeBits)) - 1;

        checkMachineId(workerId, datacenterId);
        return this;
    }

    /**
     * 获得下一个ID (该方法是线程安全的)
     * @return SnowflakeId
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        // 系统时钟回退时间
        long backOffset = lastTimestamp - timestamp;
        // 单位转换：秒
        if (timeUnit == TimeUnit.SECONDS) {
            backOffset = DateUtils.toUnixTimeOfCeil(backOffset);
        }
        if (backOffset > 0) {
            // 允许范围内休眠
            if (backOffset <= MAX_BACKWARD) {
                LockSupport.parkNanos(timeUnit.toNanos(backOffset));
            }
            // 超出抛异常
            else {
                throw new RuntimeException(String.format("Clock moved backwards. Refusing to generate id for %d %s", backOffset, timeUnit.toString()));
            }
        }
        // 如果是同一时间生成的，则进行序列递增
        if (backOffset == 0) {
            sequence = (sequence + 1) & sequenceMask;
            // 序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒|秒，获得新的时间戳
                timestamp = tilNextTime(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }
        // 上次生成ID的时间截
        lastTimestamp = timestamp;
        // 移位并通过或运算拼到一起组成64位的ID
        long timeOffset = timestamp - twepoch;
        // 单位转换：秒（往下取整：避免首次重复）
        if (timeUnit == TimeUnit.SECONDS) {
            timeOffset = DateUtils.toUnixTime(timeOffset);
        }
        return (timeOffset << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    /**
     * 阻塞到下一个毫秒|秒，直到获得新的时间戳
     * @param lastTimestamp 上次生成ID的时间截
     * @return 当前时间戳
     */
    protected long tilNextTime(long lastTimestamp) {
        long nextTimestamp = lastTimestamp + 1;
        // 单位转换：秒
        if (timeUnit == TimeUnit.SECONDS) {
            nextTimestamp = DateUtils.toUnixTimeOfCeil(nextTimestamp) * 1000L;
        }
        long timestamp = timeGen();
        while (timestamp < nextTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回以毫秒为单位的当前时间
     * @return 当前时间(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 获取id对应的时间（ms）
     * @param id
     * @return
     */
    public long getIdTimeMillis(long id) {
        long elapsedTimes = id >> timestampLeftShift;
        return twepoch + (timeUnit == TimeUnit.MILLISECONDS ? elapsedTimes : elapsedTimes * 1000L);
    }

    public synchronized String nextSn() {
        return nextSn(0);
    }

    /**
     * 获取序号：[日期][ID]
     * @return
     */
    public synchronized String nextSn(int length) {
        long id = nextId();
        // 当天所在毫秒|秒值：相对于00:00:00时刻的偏移值
        long dayTimes = timeUnit == TimeUnit.MILLISECONDS ? DateUtils.SECONDS_PER_DAY * 1000L : DateUtils.SECONDS_PER_DAY;
        long newTimeOffset = (id >> timestampLeftShift) % dayTimes;
        long newId = (newTimeOffset << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;

        // 最大长度，不足位补0
        int maxLength = String.valueOf(maxSnId).length();
        if (length > 0) {
            if (length < maxLength) {
                throw new RuntimeException(String.format("Id length is less than %d: %d", maxLength, length));
            }
            maxLength = length;
        }
        String dayFormat = DateUtils.getFormatDate(lastTimestamp, "yyyyMMdd");
        return dayFormat + StringUtils.toFixedLength(String.valueOf(newId), maxLength, '0');
    }

    @Override
    public String toString() {
        return "SnowflakeIdWorker{" +
                "workerId=" + workerId +
                ", datacenterId=" + datacenterId +
                ", workerIdBits=" + workerIdBits +
                ", datacenterIdBits=" + datacenterIdBits +
                ", sequenceBits=" + sequenceBits +
                ", timeUnit=" + timeUnit +
                '}';
    }

    public static void main(String[] args) throws InterruptedException {
        long time = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(10);
        for (int i = 0; i < latch.getCount(); i++) {
            int workId = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    printAll(new SnowflakeIdWorker(workId, 0).doSetBits(8, 2, 10, TimeUnit.SECONDS));
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        System.out.println("finish: " + (System.currentTimeMillis() - time));
    }

    public static void printAll(SnowflakeIdWorker idWorker) {
        System.out.println(idWorker);
        for (int i = 0; i < 1000; i++) {
            String sn = idWorker.nextSn() + "";
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println(sn);
        }
    }
}
