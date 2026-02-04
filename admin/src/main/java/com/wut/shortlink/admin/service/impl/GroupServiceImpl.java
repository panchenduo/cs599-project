package com.wut.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wut.shortlink.admin.dao.entity.GroupDO;
import com.wut.shortlink.admin.dao.mapper.GroupMapper;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupSaveRespDTO;
import com.wut.shortlink.admin.service.GroupService;
import com.wut.shortlink.admin.toolkit.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
    @Override
    public void saveGroup(ShortLinkGroupSaveReqDTO shortLinkGroupSaveReqDTO) {
        String gid;
        do {
            gid = RandomGenerator.generateSixLenRandomString();
        } while (hasGid(gid));
        //todo 用户名好像需要网关传过来。待实现
        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(shortLinkGroupSaveReqDTO.getName())
                .sortOrder(0)
                .build();
        baseMapper.insert(groupDO);
    }

    @Override
    public List<ShortLinkGroupSaveRespDTO> groupList() {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, "pcd")
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(wrapper);
        return BeanUtil.copyToList(groupDOList, ShortLinkGroupSaveRespDTO.class);
    }

    private Boolean hasGid(String gid) {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                //todo 用户名好像需要网关传过来。待实现
                .eq(GroupDO::getUsername, null);
        GroupDO groupDO = baseMapper.selectOne(wrapper);
        return groupDO != null;
    }
}
