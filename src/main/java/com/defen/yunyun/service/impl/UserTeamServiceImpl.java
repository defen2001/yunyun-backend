package com.defen.yunyun.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defen.yunyun.model.entity.UserTeam;
import com.defen.yunyun.service.UserTeamService;
import com.defen.yunyun.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author defen
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-09-16 16:42:39
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




