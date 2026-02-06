package com.wut.shortlink.admin.remote.dto;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkCreateReqDTO;
import com.wut.shortlink.admin.remote.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import com.wut.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

import java.util.HashMap;
import java.util.Map;

/**
 * 短链接中台远程调用服务
 */
public interface ShortLinkRemoteService {
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("gid", shortLinkPageReqDTO.getGid());
        requestMap.put("current", shortLinkPageReqDTO.getCurrent());
        requestMap.put("size", shortLinkPageReqDTO.getSize());
        String s = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page", requestMap);
        return JSON.parseObject(s, new TypeReference<>() {
        });
    }

    /**
     * 创建短链接
     *
     * @param reqDTO
     * @return
     */
    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO reqDTO) {
        String s = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create", JSON.toJSONString(reqDTO));
        return JSON.parseObject(s, new TypeReference<>() {
        });
    }
}
