package com.defen.yunyun.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.defen.yunyun.model.dto.team.*;
import com.defen.yunyun.model.entity.Team;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.vo.TeamInfoVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 队伍服务
 *
 * @author defen
 */
public interface TeamService extends IService<Team> {

    /**
     * 校验
     *
     * @param team
     */
    void validTeamInfo(Team team);
    /**
     * 创建队伍
     *
     * @param team
     * @param request
     * @return
     */
    long addTeam(Team team, HttpServletRequest request);

    /**
     * 更新队伍
     *
     * @param team
     * @param request
     * @return
     */
    void updateTeam(TeamUpdateRequest team, HttpServletRequest request);

    /**
     * 加入队伍
     *
     * @param teamJoinRequest
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request);

    /**
     * 退出队伍
     *
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    String quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest loginUser);

    /**
     * 删除（解散）队伍
     *
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id, User loginUser);

    TeamInvitation sendTeamInvitation(TeamInvitation teamInvitation, HttpServletRequest request);

    void acceptTeamInvitation(String invitationCode, HttpServletRequest request);

    TeamInfoVO queryByTeamId(Long teamId, HttpServletRequest request);

    List<TeamInfoVO> listMyTeamInfoVO(HttpServletRequest request);

    List<TeamInfoVO> listTeamInfoByUserId(Long userId);

    Page<TeamInfoVO> queryByConditionWithPagination(TeamQuery teamQuery);

    List<User> listTeamMember(Long teamId);

    Page<TeamInfoVO> recommendTeams(Long userId, Integer currentPage);
}
