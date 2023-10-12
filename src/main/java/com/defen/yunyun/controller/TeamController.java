package com.defen.yunyun.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.defen.yunyun.common.BaseResponse;
import com.defen.yunyun.common.DeleteRequest;
import com.defen.yunyun.common.ErrorCode;
import com.defen.yunyun.common.ResultUtils;
import com.defen.yunyun.exception.BusinessException;
import com.defen.yunyun.model.dto.team.*;
import com.defen.yunyun.model.entity.Team;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.entity.UserTeam;
import com.defen.yunyun.model.vo.TeamInfoVO;
import com.defen.yunyun.model.vo.TeamUserVO;
import com.defen.yunyun.service.TeamService;
import com.defen.yunyun.service.UserService;
import com.defen.yunyun.service.UserTeamService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队伍接口
 *
 * @author defen
 */
@RestController
@RequestMapping("/team")
public class TeamController {

    @Resource
    private UserService userService;

    @Resource
    private  TeamService teamService;

    @Resource
    private UserTeamService userTeamService;


    /**
     * 创建队伍
     * @param teamAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request) {
        if (teamAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, request);
        return ResultUtils.success(teamId);
    }

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.joinTeam(teamJoinRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 用户发送入队邀请
     * @param teamInvitation
     * @return
     */
    @PostMapping("/invite")
    public BaseResponse<TeamInvitation> sendTeamInvitation(@RequestBody TeamInvitation teamInvitation, HttpServletRequest request) {
        if (teamInvitation == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入邀请信息");
        return ResultUtils.success(teamService.sendTeamInvitation(teamInvitation, request));
    }

    /**
     * 用户接受入队邀请
     * @param code
     * @return
     */
    @GetMapping("/join/invite")
    public BaseResponse<String> acceptTeamInvitation(String code, HttpServletRequest request) {
        if (Strings.isBlank(code))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入邀请码");
        teamService.acceptTeamInvitation(code, request);
        return ResultUtils.success("队伍加入成功");
    }

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param request
     * @return
     */
    @PostMapping("/quit")
    public BaseResponse<String> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String result = teamService.quitTeam(teamQuitRequest, request);
        return ResultUtils.success(result);
    }

    /**
     * 更新队伍
     * @param teamUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<String> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null || teamUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        teamService.updateTeam(teamUpdateRequest, request);
        return ResultUtils.success("队伍信息修改成功");
    }

    /**
     * 获取指定队伍
     * @param teamId
     * @return
     */
    @GetMapping("/{teamId}")
    public BaseResponse<TeamInfoVO> queryByTeamId(@PathVariable Long teamId, HttpServletRequest request) {
        if (teamId == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要查询的队伍");
        return ResultUtils.success(teamService.queryByTeamId(teamId, request));
    }

    /**
     * 查询我的队伍列表
     *
     * @param request
     * @return
     */
    @GetMapping("/list/mine")
    public BaseResponse<List<TeamInfoVO>> listMyTeamInfoVO(HttpServletRequest request) {
        List<TeamInfoVO> teamList = teamService.listMyTeamInfoVO(request);
        return ResultUtils.success(teamList);
    }

    /**
     * 查询指定用户的队伍列表
     * @param userId
     * @return
     */
    @GetMapping("/list/{userId}")
    public BaseResponse<List<TeamInfoVO>> listTeamInfoByUserId(@PathVariable Long userId) {
        return ResultUtils.success(teamService.listTeamInfoByUserId(userId));
    }

    /**
     *
     * @param teamQuery
     * @return
     */
    @GetMapping("/page")
    public BaseResponse<Page<TeamInfoVO>> queryByConditionWithPagination(TeamQuery teamQuery) {
        if (teamQuery.getCurrent() < 1 || teamQuery.getPageSize() < 1)
            teamQuery.setCurrent(1);
        return ResultUtils.success(teamService.queryByConditionWithPagination(teamQuery));
    }

    /**
     *
     * @param teamId
     * @return
     */
    @GetMapping("/member/{teamId}")
    public BaseResponse<List<User>> listTeamMember(@PathVariable Long teamId) {
        return ResultUtils.success(teamService.listTeamMember(teamId));
    }

    /**
     * 分页推荐随机队伍
     * @param userId
     * @param currentPage
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<TeamInfoVO>> recommendTeams(Long userId, Integer currentPage) {
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        return ResultUtils.success(teamService.recommendTeams(userId, currentPage));
    }
}
