
package com.wut.shortlink.project.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wut.shortlink.project.dao.entity.LinkDO;
import com.wut.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.wut.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import org.apache.ibatis.annotations.Param;

/**
 * 短链接持久层
 */
public interface ShortLinkMapper extends BaseMapper<LinkDO> {

    /**
     * 短链接访问统计自增
     */
    void incrementStats(@Param("gid") String gid,
                        @Param("fullShortUrl") String fullShortUrl,
                        @Param("totalPv") Integer totalPv,
                        @Param("totalUv") Integer totalUv,
                        @Param("totalUip") Integer totalUip);

    /**
     * 分页统计短链接
     */
    IPage<LinkDO> pageLink(ShortLinkPageReqDTO requestParam);

    /**
     * 分页统计回收站短链接
     */
    IPage<LinkDO> pageRecycleBinLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
