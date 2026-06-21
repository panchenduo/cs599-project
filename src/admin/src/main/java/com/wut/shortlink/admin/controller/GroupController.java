package com.wut.shortlink.admin.controller;

import com.wut.shortlink.admin.common.convention.result.Result;
import com.wut.shortlink.admin.common.convention.result.Results;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupSortDTO;
import com.wut.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.wut.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.wut.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 短链接分组控制层
 */
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
    /**
     * 新增短链接分组
     */
    @PostMapping("/api/short-link/admin/v1/group")
    public Result<Void> saveGroup(@RequestBody ShortLinkGroupSaveReqDTO shortLinkGroupSaveReqDTO) {
        groupService.saveGroup(shortLinkGroupSaveReqDTO.getName());
        return Results.success();
    }
    /**
     * 获取短链接分组列表
     */
    @GetMapping("/api/short-link/admin/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> groupList() {
        return Results.success(groupService.groupList());
    }
    /**
     * 修改短链接分组
     */
    @PutMapping("/api/short-link/admin/v1/group")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO shortLinkGroupUpdateReqDTO) {
        groupService.updateGroup(shortLinkGroupUpdateReqDTO);
        return Results.success();
    }
    /**
     * 删除短链接分组
     */
    @DeleteMapping("/api/short-link/admin/v1/group")
    public Result<Void> deleteGroup(@RequestParam("gid") String gid) {
        groupService.deleteGroup(gid);
        return Results.success();
    }
    /**
     * 排序短链接分组
     */
    @PostMapping("/api/short-link/admin/v1/group/sort")
    public Result<Void> sortGroup(@RequestBody List<ShortLinkGroupSortDTO> shortLinkGroupSortDTO) {
        groupService.sortGroup(shortLinkGroupSortDTO);
        return Results.success();
    }
}
