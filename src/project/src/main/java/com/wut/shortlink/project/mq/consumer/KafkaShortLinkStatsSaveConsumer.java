package com.wut.shortlink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wut.shortlink.project.dao.entity.*;
import com.wut.shortlink.project.dao.mapper.*;
import com.wut.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.wut.shortlink.project.mq.idempotent.MessageQueueIdempotentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.wut.shortlink.project.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static com.wut.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Slf4j

@Component

@RequiredArgsConstructor

public class KafkaShortLinkStatsSaveConsumer {

    private final ShortLinkMapper shortLinkMapper;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final MessageQueueIdempotentHandler messageQueueIdempotentHandler;
    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;
    /**
     * @KafkaListener 这个注解极其强大，它会让这个方法像雷达一样，一直盯着 Kafka 里的主题看
     * topics = "short-link-stats-topic" (盯着我们刚才发消息的那个频道)
     * groupId = "short-link-stats-group" (认领自己的消费者组代号)
     */
    @KafkaListener(topics = "short-link-stats-topic", groupId = "short-link-stats-group")
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {
        // 1. 把快递盒子拆开，拿出里面的 JSON 文本
        String messageValue = record.value();
        // 2. 把 JSON 文本重新变回我们熟悉的 Java 对象
        ShortLinkStatsRecordDTO statsRecord = JSON.parseObject(messageValue, ShortLinkStatsRecordDTO.class);
        // 3. 拿出我们自带的唯一标识，准备做防重复校验
        String idempotentKey = statsRecord.getKeys();
        try {
            // ---- 以下几乎完全复用你原来的优秀逻辑 ----
            if (!messageQueueIdempotentHandler.isMessageProcessed(idempotentKey)) {
                // 如果发现之前处理过，并且已经彻底完工了
                if (messageQueueIdempotentHandler.isAccomplish(idempotentKey)) {
                    // 【关键改变】不抛异常，而是告诉 Kafka：这条重发的消息我之前处理过了，算我消费成功了，把进度条往后挪吧！
                    ack.acknowledge();
                    return;
                }
                // 如果发现之前处理过，但没完工，说明上次执行一半断了，抛个异常让它等会儿再重试
                throw new RuntimeException("消息未完成流程，需要消息队列重试");
            }
            // 4. 这里调用你真正去写 MySQL 数据库的方法
             actualSaveShortLinkStats(statsRecord);
            // 5. 数据库写完了！去 Redis 里打个勾，标记这个 keys 已经彻底搞定
            messageQueueIdempotentHandler.setAccomplish(idempotentKey);
            // ---- 复用逻辑结束 ----
            // 6. 【绝对的核心】业务全部走完，没有任何报错。正式向 Kafka 提交确认签收！
            // 只要执行了这行代码，Kafka 里的消费进度指针就会往后走，下次绝对不会再发这条旧消息给你了。
            // 这就完美替代了你原来 Redis 里 opsForStream().delete() 的功能。
            ack.acknowledge();
        } catch (Throwable ex) {
            // 如果在上面存数据库的过程中，数据库突然挂了，走到了这里...
            // 1. 把刚才打的正在处理的标记删掉，给下次重试留出机会
            messageQueueIdempotentHandler.delMessageProcessed(idempotentKey);
            log.error("写数据库时发生了惨烈的宕机！短链 keys: {}", idempotentKey, ex);
            // 2. 把异常继续往上抛。
            // Spring Kafka 框架看到你抛了异常，就知道你没成功。
            // 它【绝对不会】执行 ack.acknowledge() 往后挪指针，过一会儿它会把这条消息重新发给你再试一次。
            throw ex;
        }

    }
    // 加上事务注解，保证这 9 张表要么全成功，要么全失败
    @Transactional(rollbackFor = Exception.class)
    public void actualSaveShortLinkStats(ShortLinkStatsRecordDTO statsRecord) {
        String fullShortUrl = statsRecord.getFullShortUrl();
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            String gid = shortLinkGotoDO.getGid();
            int hour = DateUtil.hour(new Date(), true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .uip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .hour(hour)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocaleAmapKey);
            localeParamMap.put("ip", statsRecord.getRemoteAddr());
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String infoCode = localeResultObj.getString("infocode");
            String actualProvince = "未知";
            String actualCity = "未知";
            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                String province = localeResultObj.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .province(actualProvince = unknownFlag ? actualProvince : province)
                        .city(actualCity = unknownFlag ? actualCity : localeResultObj.getString("city"))
                        .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .country("中国")
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinkLocaleState(linkLocaleStatsDO);
            }
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(statsRecord.getOs())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkOsStatsMapper.shortLinkOsState(linkOsStatsDO);
            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(statsRecord.getBrowser())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(statsRecord.getDevice())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(statsRecord.getNetwork())
                    .cnt(1)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .user(statsRecord.getUv())
                    .ip(statsRecord.getRemoteAddr())
                    .browser(statsRecord.getBrowser())
                    .os(statsRecord.getOs())
                    .network(statsRecord.getNetwork())
                    .device(statsRecord.getDevice())
                    .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
                    .fullShortUrl(fullShortUrl)
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);
            shortLinkMapper.incrementStats(gid, fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);
            LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                    .todayPv(1)
                    .todayUv(statsRecord.getUvFirstFlag() ? 1 : 0)
                    .todayUip(statsRecord.getUipFirstFlag() ? 1 : 0)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
        } catch (Throwable ex) {
            log.error("短链接访问量统计异常", ex);
        } finally {
            rLock.unlock();
        }
    }
}