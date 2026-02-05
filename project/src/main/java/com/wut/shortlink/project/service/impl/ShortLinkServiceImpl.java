package com.wut.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wut.shortlink.project.common.convention.exception.ServiceException;
import com.wut.shortlink.project.dao.entity.LinkDO;
import com.wut.shortlink.project.dao.mapper.ShortLinkMapper;
import com.wut.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.project.service.ShortLinkService;
import com.wut.shortlink.project.toolkit.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 短链接服务接口实现层
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, LinkDO> implements ShortLinkService {
    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO reqDTO) {
        String shortLinkSuffix = generateSuffix(reqDTO);
        String fullShortLink = reqDTO.getDomain() + "/" + shortLinkSuffix;
        LinkDO linkDO = BeanUtil.copyProperties(reqDTO, LinkDO.class);
        linkDO.setShortUri(shortLinkSuffix);
        linkDO.setFullShortUrl(fullShortLink);
        linkDO.setEnableStatus(0);
        try {
            baseMapper.insert(linkDO);
            shortUriCreateCachePenetrationBloomFilter.add(shortLinkSuffix);
        } catch (DuplicateKeyException e) {
            //为什么通过布隆过滤器之后，在数据库还能遇到冲突？
            //高并发场景下，多个请求同时通过布隆过滤器，导致重复数据库添加重复信息。
            log.warn("短链接已存在{}", fullShortLink);
            //已经误判的短链接该如何处理
            throw new ServiceException("短链接已存在");
        }
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(linkDO.getFullShortUrl())
                .originUrl(linkDO.getOriginUrl())
                .gid(linkDO.getGid())
                .build();
    }

    private String generateSuffix(ShortLinkCreateReqDTO reqDTO) {
        String shortLinkSuffix;
        int customGenerateCount = 0;
        while (true) {
            if (customGenerateCount > 10) {
                throw new RuntimeException("短链接频繁生成，请稍后再试");
            }
            String originUrl = reqDTO.getOriginUrl();
            originUrl += UUID.randomUUID().toString();
            shortLinkSuffix = HashUtil.hashToBase62(originUrl);
            boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(reqDTO.getDomain() + "/" + shortLinkSuffix);
            if (!contains) {
                return shortLinkSuffix;
            }
            customGenerateCount++;
        }
    }
}
