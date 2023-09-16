package com.defen.yunyun.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defen.yunyun.mapper.TeamMapper;
import com.defen.yunyun.model.dto.team.TeamJoinRequest;
import com.defen.yunyun.model.dto.team.TeamQuery;
import com.defen.yunyun.model.dto.team.TeamQuitRequest;
import com.defen.yunyun.model.dto.team.TeamUpdateRequest;
import com.defen.yunyun.model.entity.Team;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.vo.TeamUserVO;
import com.defen.yunyun.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 队伍服务实现类
 *
 * @author defen
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Override
    public long addTeam(Team team, User loginUser) {
        return 0;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        return null;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        return false;
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        return false;
    }

    @Override
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        return false;
    }

    @Override
    public boolean deleteTeam(long id, User loginUser) {
        return false;
    }
}




