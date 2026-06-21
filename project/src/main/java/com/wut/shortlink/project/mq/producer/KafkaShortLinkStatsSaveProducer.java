package com.wut.shortlink.project.mq.producer;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson2.JSON;
import com.wut.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j

@Component

@RequiredArgsConstructor

public class KafkaShortLinkStatsSaveProducer {
    // 1. 注入 Kafka 的发送工具（完美平替你原来的 StringRedisTemplate）
    private final KafkaTemplate<String, String> kafkaTemplate;
    // 2. 提前规定好一个主题（Topic）名字，就像是一个专门收发短链统计的“公共频道”
    private static final String TOPIC_NAME = "short-link-stats-topic";

    /**
     * 发送短链接统计（业务方直接调这个方法就行）
     */
    public void shortLinkStats(ShortLinkStatsRecordDTO statsRecord) {
        // 1. 【架构重塑】提取“短链 URL”作为 Kafka 的路由 Key！
        // 这样能保证同一个短链的所有点击日志，全部乖乖排队进入同一个 Partition
        String kafkaRoutingKey = statsRecord.getFullShortUrl();

        // 2. 将整个 DTO（包括里面那个极其重要的用来做防重的 keys）变成 JSON
        // 给 DTO 添加一个 UUID，用来做防重用的 keys
        statsRecord.setKeys(UUID.fastUUID().toString());
        // 注意：那个防重用的 keys，依然安全地躺在 JSON 肚子里，等着消费者拆快递拿出来用
        String messageValue = JSON.toJSONString(statsRecord);
        // 3. 正式发射！(Topic, 路由 Key, 包含业务数据和防重 ID 的 Value)
        kafkaTemplate.send("short-link-stats-topic", kafkaRoutingKey, messageValue)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("短链统计记录发送 Kafka 失败, url: {}", kafkaRoutingKey, ex);
                    }
                });
    }
}