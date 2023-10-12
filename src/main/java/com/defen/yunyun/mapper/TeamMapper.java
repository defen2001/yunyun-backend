package com.defen.yunyun.mapper;

import com.defen.yunyun.model.entity.Team;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.defen.yunyun.model.vo.TeamInfoVO;

import java.util.List;

/**
* @author defen
* @description 针对表【team(队伍)】的数据库操作Mapper
* @createDate 2023-09-16 16:42:31
* @Entity com.defen.yunyun.model.entity.Team
*/
public interface TeamMapper extends BaseMapper<Team> {

    TeamInfoVO getTeamInfoById(Long teamId);

    List<TeamInfoVO> listAllTeamInfoByUserId(Long userId);

    List<TeamInfoVO> listTeamInfoByUserId(Long userId);

    List<TeamInfoVO> listTeamInfoByCondition(long offset, long limit, String searchText, boolean onlyNoPassword);

    long countTeamByCondition(String searchText, boolean onlyNoPassword);
}




