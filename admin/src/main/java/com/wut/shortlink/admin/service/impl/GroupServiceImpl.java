package com.wut.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wut.shortlink.admin.common.biz.user.UserContext;
import com.wut.shortlink.admin.common.convention.exception.ClientException;
import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.dao.entity.GroupDO;
import com.wut.shortlink.admin.dao.mapper.GroupMapper;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSortDTO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.wut.shortlink.admin.remote.ShortLinkRemoteService;
import com.wut.shortlink.admin.service.GroupService;
import com.wut.shortlink.admin.toolkit.RandomGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {
    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService() {
    };

    @Override
    public void saveGroup(String groupName) {
        String username = UserContext.getUsername();
        saveGroup(username, groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
        String gid;
        do {
            gid = RandomGenerator.generateSixLenRandomString();
        } while (hasGid(username,gid));
        GroupDO groupDO = GroupDO.builder()
                .gid(gid)
                .name(groupName)
                .username(username)
                .sortOrder(0)
                .build();
        baseMapper.insert(groupDO);
    }

    @Override
    public List<ShortLinkGroupRespDTO> groupList() {
        LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getDelFlag, 0)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime);
        List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkRemoteService
                .listGroupShortLinkCount(groupDOList.stream().map(GroupDO::getGid).toList());
        List<ShortLinkGroupRespDTO> shortLinkGroupRespDTOList = BeanUtil.copyToList(groupDOList, ShortLinkGroupRespDTO.class);
        shortLinkGroupRespDTOList.forEach(each -> {
            Optional<ShortLinkGroupCountQueryRespDTO> first = listResult.getData().stream()
                    .filter(item -> Objects.equals(item.getGid(), each.getGid()))
                    .findFirst();
            first.ifPresent(item -> each.setShortLinkCount(String.valueOf(first.get().getShortLinkCount())));
        });
        return shortLinkGroupRespDTOList;
    }

    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO shortLinkGroupUpdateReqDTO) {
        boolean updated = lambdaUpdate()
                .eq(GroupDO::getGid, shortLinkGroupUpdateReqDTO.getGid())
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .set(GroupDO::getName, shortLinkGroupUpdateReqDTO.getName())
                .update();
        if (!updated) {
            throw new ClientException("更新失败！");
        }
    }

    @Override
    public void deleteGroup(String gid) {
        boolean deleted = lambdaUpdate()
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .set(GroupDO::getDelFlag, "1")
                .update();
        if (!deleted) {
            throw new ClientException("删除失败！");
        }
    }

    @Override
    public void sortGroup(List<ShortLinkGroupSortDTO> shortLinkGroupSortDTO) {
        shortLinkGroupSortDTO.forEach(item -> {
            boolean updated = lambdaUpdate()
                    .eq(GroupDO::getGid, item.getGid())
                    .eq(GroupDO::getUsername, UserContext.getUsername())
                    .eq(GroupDO::getDelFlag, 0)
                    .set(GroupDO::getSortOrder, item.getSortOrder())
                    .update();
            if (!updated) {
                throw new ClientException("排序失败！");
            }
        });
    }

    private Boolean hasGid(String username, String gid) {
        LambdaQueryWrapper<GroupDO> wrapper = Wrappers.lambdaQuery(GroupDO.class)
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, Optional.ofNullable(username).orElse(UserContext.getUsername()));
        GroupDO groupDO = baseMapper.selectOne(wrapper);
        return groupDO != null;
    }
}
