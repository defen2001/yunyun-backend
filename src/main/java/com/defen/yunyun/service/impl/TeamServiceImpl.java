package com.defen.yunyun.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defen.yunyun.common.ErrorCode;
import com.defen.yunyun.constant.RedisConstant;
import com.defen.yunyun.exception.BusinessException;
import com.defen.yunyun.mapper.TeamMapper;
import com.defen.yunyun.model.dto.team.*;
import com.defen.yunyun.model.entity.Team;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.entity.UserTeam;
import com.defen.yunyun.model.enums.TeamTypeEnum;
import com.defen.yunyun.model.vo.TeamInfoVO;
import com.defen.yunyun.service.TeamService;
import com.defen.yunyun.service.UserService;
import com.defen.yunyun.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.defen.yunyun.constant.SystemConstant.*;

/**
 * 队伍服务实现类
 *
 * @author defen
 */
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private TeamMapper teamMapper;
    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 校验
     *
     * @param team
     */
    @Override
    public void validTeamInfo(Team team) {
        // 1. 请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        int memberLimit = Optional.ofNullable(team.getMemberLimit()).orElse(10);
        if (memberLimit < 2 || memberLimit > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数限制在 2~10 人之间");
        }
        //   2. 队伍标题 <= 20
        final String TEAM_NAME_REGEX = "^[\\u4e00-\\u9fa5_a-zA-Z0-9]{2,20}$";
        String name = team.getName();
        if (StringUtils.isBlank(name) || !name.matches(TEAM_NAME_REGEX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队名必须由 2~20 位的中英文或数字组成");
        }
        //   3. 描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述限制 50 字以内");
        }
        //   4. status 是否公开（int）不传默认为 0（公开）
        int type = Optional.ofNullable(team.getType()).orElse(0);
//        TeamTypeEnum statusEnum = TeamTypeEnum.getEnumByValue(type);
        if (type != 0 && type != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍类型不符合要求");
        }
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (type != 0 && Strings.isNotBlank(password)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "仅公开队伍可设置入队密码");

        }
//        if (TeamTypeEnum.SECRET.equals(statusEnum)) {
//            if (StringUtils.isBlank(password)) {
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码设置不正确");
//            }
//        }
        // 6. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }

    }

    /**
     * 创建队伍
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, HttpServletRequest request) {
        // 1. 校验参数
        validTeamInfo(team);
        // 2. 是否登录，未登录不允许创建
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = loginUser.getId();
        // 7. 校验用户最多创建 5 个队伍
        // todo 有 bug，可能同时创建 100 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("leaderId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= OWNED_TEAM_LIMIT) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "您拥有的队伍数量已达上限");
        }
        // 8. 插入队伍信息到队伍表
        team.setId(null);
        team.setLeaderId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 9. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        // 发送消息
        userService.sendMessages(String.format(CREATE_TEAM_MESSAGE,team.getName()),
                new HashSet<>(Arrays.asList(userId)),
                request);
        return teamId;
    }

    @Override
    public void updateTeam(TeamUpdateRequest teamUpdateRequest, HttpServletRequest request) {
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = validateTeamExist(teamUpdateRequest.getId(), false);
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        // 只有管理员或者队伍的创建者可以修改
        if (!loginUser.getId().equals(oldTeam.getLeaderId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅队长可修改队伍信息");
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        // 校验队伍信息
        validTeamInfo(team);
        // 校验当前队伍人数是否超过人数限制
        int teamMembers = listTeamMember(team.getId()).size();
        if (teamMembers > team.getMemberLimit())
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "当前人数已超过该人数限制");

        this.lambdaUpdate()
                .eq(Team::getId, team.getId())
                .set(Team::getName, team.getName())
                .set(Team::getDescription, team.getDescription())
                .set(Team::getType, team.getType())
                .set(team.getType() == 0 && Strings.isNotBlank(team.getPassword())
                        , Team::getPassword, team.getPassword())
                .set(team.getType() != 0, Team::getPassword, null)
                .set(Team::getMemberLimit, team.getMemberLimit())
                .set(Team::getExpireTime, team.getExpireTime())
                .update();

        // 发送消息
        Set<Long> userIds = listTeamMember(team.getId())
                .stream()
                .map(User::getId)
                .filter(id -> !id.equals(userId))
                .collect(Collectors.toSet());
        userService.sendMessages(String.format(
                UPDATE_TEAM_INFO_MESSAGE, oldTeam.getName()), userIds, request);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, HttpServletRequest request) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getType();
        TeamTypeEnum teamTypeEnum = TeamTypeEnum.getEnumByValue(status);
        if (TeamTypeEnum.PRIVATE.equals(teamTypeEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamTypeEnum.SECRET.equals(teamTypeEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        User loginUser = userService.getLoginUser(request);
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("yupao:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    // 已加入队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMemberLimit()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
            // 发送消息
            User user = userService.getById(userId);
            String username = Optional.ofNullable(user.getUsername())
                    .orElse(user.getUserAccount());
            userService.sendMessages(String.format(JOIN_TEAM_MESSAGE,
                    username, team.getName()),
                    new HashSet<>(Arrays.asList(team.getLeaderId())),
                    request);
        }
    }

    /**
     * 退出或解散队伍
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String quitTeam(TeamQuitRequest teamQuitRequest, HttpServletRequest request) {
        if (teamQuitRequest == null || teamQuitRequest.getTeamId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamQuitRequest.getTeamId();
//        Team team = getTeamById(teamId);
        Team team = validateTeamExist(teamId, false);
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        List<UserTeam> userTeams = userTeamService.lambdaQuery()
                .eq(UserTeam::getTeamId, teamId)
                .list();
        if (userTeams.stream()
                .map(UserTeam::getUserId)
                .noneMatch(userId::equals)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您未在该队伍中");
        }

        Long leaderId = team.getLeaderId();
        if (leaderId.equals(userId)) {
            // 解散队伍
            this.removeById(teamId);
            userTeamService.lambdaUpdate()
                    .eq(UserTeam::getTeamId, teamId)
                    .remove();
            // 发送消息
            User leader = userService.getById(userId);
            String leaderName = Optional.ofNullable(leader.getUsername())
                    .orElse(leader.getUserAccount());
            Set<Long> userIds = userTeams.stream()
                    .map(UserTeam::getUserId)
                    .filter(id -> !id.equals(userId))
                    .collect(Collectors.toSet());
            userService.sendMessages(
                    String.format(DISBAND_TEAM_MESSAGE, leaderName, team.getName()),
                    userIds, request);
            return "已解散队伍";
        } else {
            // 退出队伍
            userTeamService.lambdaUpdate()
                    .eq(UserTeam::getUserId, userId)
                    .eq(UserTeam::getTeamId, teamId)
                    .remove();
            // 发送消息
            User user = userService.getById(userId);
            String username = Optional.ofNullable(user.getUsername())
                    .orElse(user.getUserAccount());
            userService.sendMessages(String.format(
                    QUIT_TEAM_MESSAGE, username, team.getName()),
                    new HashSet<>(Arrays.asList(leaderId)),
                    request);
            return "已退出队伍";
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(long id, User loginUser) {
        // 校验队伍是否存在
        Team team = getTeamById(id);
        long teamId = team.getId();
        // 校验你是不是队伍的队长
        if (team.getLeaderId() != loginUser.getId()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无访问权限");
        }
        // 移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(userTeamQueryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍
        return this.removeById(teamId);
    }

    /**
     * 发送入队邀请
     */
    @Override
    public TeamInvitation sendTeamInvitation(TeamInvitation teamInvitation, HttpServletRequest request) {
        // 校验邀请信息
        Long teamId = teamInvitation.getTeamId();
        Long invitee = teamInvitation.getInvitee();
        if (teamId == null || invitee == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请信息不完整");

        // 校验用户是否为队长
        Team team = validateTeamExist(teamId, false);
        User loginUser = userService.getLoginUser(request);
        Long inviter = loginUser.getId();
        teamInvitation.setInviter(inviter);
        if (!inviter.equals(team.getLeaderId()))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "仅队长可使用邀请功能");

        // 校验对方是否已在队伍中
        if (userTeamService.lambdaQuery()
                .eq(UserTeam::getUserId, invitee)
                .eq(UserTeam::getTeamId, teamId)
                .one() != null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "对方已在队伍中");

        validateTeamJoinable(team);

        // 生成邀请码并保存邀请信息
        String invitationCode = RandomUtil.randomNumbers(6);
        RBucket<TeamInvitation> teamInvitationBucket = redissonClient.getBucket(
                RedisConstant.TEAM_INVITATION_KEY + invitationCode);
        while (teamInvitationBucket.isExists()) {
            invitationCode = RandomUtil.randomNumbers(6);
            teamInvitationBucket = redissonClient.getBucket(RedisConstant.TEAM_INVITATION_KEY + invitationCode);
        }
        teamInvitationBucket.set(teamInvitation, RedisConstant.TEAM_INVITATION_TTL.toMillis(), TimeUnit.MILLISECONDS);

        // 发送消息
        User leader = userService.getById(inviter);
        String leaderName = Optional.ofNullable(leader.getUsername())
                .orElse(leader.getUserAccount());
        userService.sendMessages(
                String.format(SEND_TEAM_INVITATION_MESSAGE, leaderName, team.getName(), invitationCode),
                new HashSet<>(Arrays.asList(invitee)),
                request);

        return new TeamInvitation().setInvitationCode(invitationCode);
    }

    /**
     * 接受入队邀请
     */
    @Override
    public void acceptTeamInvitation(String invitationCode, HttpServletRequest request) {
        // 校验邀请码
        RBucket<TeamInvitation> teamInvitationBucket = redissonClient.getBucket(
                RedisConstant.TEAM_INVITATION_KEY + invitationCode);
        if (!teamInvitationBucket.isExists())
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "邀请码不存在或已过期");

        TeamInvitation teamInvitation = teamInvitationBucket.get();
        Long teamId = teamInvitation.getTeamId();
        Long inviter = teamInvitation.getInviter();
        Long invitee = teamInvitation.getInvitee();

        User loginUser = userService.getLoginUser(request);
        // 校验用户是否为受邀者
        if (!invitee.equals(loginUser.getId()))
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "您不是该邀请码的受邀者");

        Team team = validateTeamExist(teamId, false);
        validateTeamJoinable(team);
        doJoinTeam(invitee, teamId);

        // 删除邀请信息
        teamInvitationBucket.delete();

        // 发送消息
        User user = userService.getById(invitee);
        String username = Optional.ofNullable(user.getUsername())
                .orElse(user.getUserAccount());
        userService.sendMessages(
                String.format(ACCEPT_TEAM_INVITATION_MESSAGE, username, team.getName()),
                new HashSet<>(Arrays.asList(invitee)),
                request);
    }

    /**
     * 根据 ID 查询队伍信息
     */
    @Override
    public TeamInfoVO queryByTeamId(Long teamId, HttpServletRequest request) {
        TeamInfoVO teamInfo = teamMapper.getTeamInfoById(teamId);
        if (teamInfo == null)
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在");
        User loginUser = userService.getLoginUser(request);
        // 私密队伍校验身份
        if (teamInfo.getType() == 1 &&
                listTeamMember(teamId)
                        .stream()
                        .noneMatch(user -> user.getId().equals(loginUser.getId())))
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在");

        return teamInfo;
    }

    /**
     * 查询我加入的队伍列表
     */
    @Override
    public List<TeamInfoVO> listMyTeamInfoVO(HttpServletRequest request) {
        return teamMapper.listAllTeamInfoByUserId(userService.getLoginUser(request).getId());
    }

    /**
     * 查询用户加入的队伍列表
     */
    @Override
    public List<TeamInfoVO> listTeamInfoByUserId(Long userId) {
        return teamMapper.listTeamInfoByUserId(userId);
    }

    /**
     * 按条件分页查询队伍信息
     */
    @Override
    public Page<TeamInfoVO> queryByConditionWithPagination(TeamQuery teamQuery) {
        // 处理查询条件
        long currentPage = teamQuery.getCurrent();
        long offset = (currentPage - 1) * DEFAULT_PAGE_SIZE;
        int limit = DEFAULT_PAGE_SIZE;
        String searchText = teamQuery.getSearchText();
        boolean onlyNoPassword = Boolean.TRUE.equals(teamQuery.getOnlyNoPassword());

        // 查询队伍信息
        List<TeamInfoVO> teamInfoList = teamMapper.listTeamInfoByCondition(
                offset, limit, searchText, onlyNoPassword);
        long total = teamMapper.countTeamByCondition(searchText, onlyNoPassword);

        Page<TeamInfoVO> teamInfoPage = new Page<>(currentPage, DEFAULT_PAGE_SIZE, total);
        return teamInfoPage.setRecords(teamInfoList);
    }

    /**
     * 查询队伍成员列表
     */
    @Override
    public List<User> listTeamMember(Long teamId) {
        List<Long> memberIds = userTeamService.lambdaQuery()
                .select(UserTeam::getUserId)
                .eq(UserTeam::getTeamId, teamId)
                .orderByAsc(UserTeam::getCreateTime)
                .list()
                .stream()
                .map(UserTeam::getUserId)
                .collect(Collectors.toList());
        return userService.queryByIdsWithCache(memberIds);
    }

    /**
     * 分页推荐随机队伍
     */
    @Override
    public Page<TeamInfoVO> recommendTeams(Long userId, Integer currentPage) {
        if (userId == null)
            userId = 0L;

        // 从缓存中获取推荐队伍列表
        RList<TeamInfoVO> teamRecommendList = redissonClient.getList(RedisConstant.TEAM_RECOMMEND_KEY + userId);
        // 游客缓存的队伍推荐列表过期或用户刷新列表时重新生成缓存
        if (teamRecommendList.isEmpty() || (userId != 0 && currentPage == 1))
            teamRecommendList = generateTeamRecommendCache(userId);

        int offset = (currentPage - 1) * DEFAULT_PAGE_SIZE;
        int total = teamRecommendList.size();

        List<TeamInfoVO> teamRecords = new ArrayList<>(teamRecommendList.subList(
                offset, Math.min(offset + DEFAULT_PAGE_SIZE, total)));
        return new Page<TeamInfoVO>(currentPage, DEFAULT_PAGE_SIZE, total).setRecords(teamRecords);
    }

    /**
     * 根据 id 获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍当前人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }

    /**
     * 校验队伍是否存在
     */
    private Team validateTeamExist(Long teamId, boolean needPublic) {
        Team team = this.getById(teamId);
        if (team == null){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在");
        }
        if (needPublic && team.getType() != 0){
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 校验队伍是否可加入
     */
    private void validateTeamJoinable(Team team) {
        // 校验队伍是否过期
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }

        // 校验队伍是否已满
        if (userTeamService.lambdaQuery()
                .eq(UserTeam::getTeamId, team.getId())
                .count() >= team.getMemberLimit()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数已满");
        }
    }

    /**
     * 执行加入队伍操作
     */
    private void doJoinTeam(Long userId, Long teamId) {
        // 获取分布式锁实例
        RLock lock = redissonClient.getLock(RedisConstant.LOCK_TEAM_JOIN_KEY + userId + ":" + teamId);
        try {
            if (lock.tryLock()) {
                // 查询用户已加入的队伍
                List<UserTeam> joinedTeams = userTeamService.lambdaQuery()
                        .eq(UserTeam::getUserId, userId)
                        .list();

                // 校验用户是否已在队伍中
                if (joinedTeams.stream()
                        .map(UserTeam::getTeamId)
                        .anyMatch(id -> id.equals(teamId)))
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "您已在队伍中");

                // 校验用户已加入的队伍数量
                if (joinedTeams.size() >= JOINED_TEAM_LIMIT)
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "您加入的队伍数量已达上限");

                // 添加用户队伍关联
                UserTeam userTeam = new UserTeam();
                userTeam.setUserId(userId);
                userTeam.setTeamId(teamId);
                userTeamService.save(userTeam);
            }
        } finally {
            // 释放锁
            if (lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

    /**
     * 生成队伍推荐列表缓存
     */
    private RList<TeamInfoVO> generateTeamRecommendCache(Long userId) {
        // 在随机位置查询队伍信息并打乱
        int offset = 0;
//        int offset = RandomUtil.randomInt(0, 100);
        List<TeamInfoVO> teamInfoList = teamMapper.listTeamInfoByCondition(
                offset, 100, null, true);
        Collections.shuffle(teamInfoList);

        // 缓存打乱后的队伍推荐列表
        RList<TeamInfoVO> recommendTeamList = redissonClient.getList(RedisConstant.TEAM_RECOMMEND_KEY + userId);
        recommendTeamList.clear();
        recommendTeamList.addAll(teamInfoList);
        recommendTeamList.expire(RedisConstant.TEAM_RECOMMEND_TTL);
        return recommendTeamList;
    }
}




