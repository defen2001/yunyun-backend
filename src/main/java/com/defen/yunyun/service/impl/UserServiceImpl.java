package com.defen.yunyun.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defen.yunyun.common.ErrorCode;
import com.defen.yunyun.constant.CommonConstant;
import com.defen.yunyun.constant.RedisConstant;
import com.defen.yunyun.constant.SystemConstant;
import com.defen.yunyun.constant.UserConstant;
import com.defen.yunyun.exception.BusinessException;
import com.defen.yunyun.mapper.UserMapper;
import com.defen.yunyun.model.dto.Message;
import com.defen.yunyun.model.dto.user.UserLogin;
import com.defen.yunyun.model.dto.user.UserPasswordRequest;
import com.defen.yunyun.model.dto.user.UserQueryRequest;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.vo.LoginUserVO;
import com.defen.yunyun.model.vo.UserVO;
import com.defen.yunyun.service.UserService;
import com.defen.yunyun.utils.IdUtils;
import com.defen.yunyun.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.redisson.api.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.defen.yunyun.constant.SystemConstant.USERNAME_REGEX;

/**
 * 用户服务实现类
 *
 * @author defen
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;


    @Resource
    private RedissonClient redissonClient;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "defen";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUsername(SystemConstant.USER_ACCOUNT_PREFIX + RandomUtil.randomString(10));
            user.setUserPassword(encryptPassword);
            user.setAvatarUrl("\thttps://image-fenapi-1319981817.cos.ap-guangzhou.myqcloud.com/imageHost/PU2babt2-43314997.png");
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public UserLogin userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        // 生成 Token
        String token = UUID.randomUUID().toString(true);
        // 保存登录状态
        User tokenUser = new User();
        tokenUser.setId(user.getId());
        tokenUser.setUserAccount(user.getUserAccount());
        tokenUser.setUserRole(user.getUserRole());
        RBucket<User> userBucket = redissonClient.getBucket(RedisConstant.USER_TOKEN_KEY + token);
        userBucket.set(tokenUser, RedisConstant.USER_TOKEN_TTL.toMillis(), TimeUnit.MILLISECONDS);
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        return new UserLogin(this.getLoginUserVO(user), token);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && UserConstant.ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取脱敏的已登录用户信息
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String username = userQueryRequest.getUsername();
        String profile = userQueryRequest.getProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(username), "userProfile", username);
        queryWrapper.like(StringUtils.isNotBlank(profile), "userName", profile);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 修改密码
     */
    @Override
    public void updatePassword(UserPasswordRequest userPasswordRequest, HttpServletRequest request) {
        // 检验
        String oldPassword = userPasswordRequest.getOldPassword();
        String password = userPasswordRequest.getPassword();
        String confirmPassword = userPasswordRequest.getConfirmPassword();
        if (StringUtils.isAnyBlank(oldPassword, password, confirmPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        if (password.length() < 8 || confirmPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!password.equals(confirmPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        User loginUser = this.getLoginUser(request);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", loginUser.getId());
        User user = userMapper.selectOne(queryWrapper);
        // 判断旧密码是否正确
        String encryptOldPassword = DigestUtils.md5DigestAsHex((SALT + oldPassword).getBytes());
        String userPassword = user.getUserPassword();
        if (!encryptOldPassword.equals(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码输入错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        // 3.更新

        this.lambdaUpdate()
                .eq(User::getId, loginUser.getId())
                .set(User::getUserPassword, encryptPassword)
                .update();
    }

    /**
     * 更新基本信息
     */
    @Override
    public boolean updateMyUser(User user) {
        // 校验
        String username = user.getUsername();
        if (username != null && !username.matches(USERNAME_REGEX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "昵称必须由 2~10 位的中英文或数字组成");
        }
        Integer gender = user.getGender();
        if (gender != null && (gender < 0 || gender > 2)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "性别参数错误");
        }
        String profile = user.getProfile();
        if (profile != null && profile.length() > 50){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "个人简介最长为 50 个字符");
        }
        String email = user.getEmail();
        if (email != null && !email.matches(SystemConstant.EMAIL_REGEX)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
        String phone = user.getPhone();
        if (phone != null && !phone.matches(SystemConstant.PHONE_NUMBER_REGEX)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        return this.updateById(user);
    }

    @Override
    public void updateTags(List<String> tags, User loginUser) {
        // 标签去重
        tags = tags.stream().distinct().collect(Collectors.toList());
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多添加 10 个标签");
        }
        Long userId = loginUser.getId();
        this.lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getTags, JSONUtil.toJsonStr(tags))
                .update();
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("id", userId);
//        // 使用Gson库将标签列表转换为JSON字符串
//        User userToUpdate = new User();
//        userToUpdate.(JSONUtil.toJsonStr(tags));
//
//        this.update(userToUpdate, queryWrapper);
        // 删除缓存
        redissonClient.getBucket(RedisConstant.USER_INFO_KEY + userId)
                .delete();
    }

    /**
     * 根据 ID 查询用户信息
     */
    @Override
    public User queryByUserId(Long userId) {
        UserVO userVO = getUserVO(this.getById(userId));
        User user = new User();
        BeanUtils.copyProperties(userVO, user);
        redissonClient.getBucket(RedisConstant.USER_INFO_KEY + userId)
                .set(user, RedisConstant.USER_INFO_TTL.toMillis(), TimeUnit.MILLISECONDS);
        return user;
    }

    @Override
    public Page<User> queryByTagsWithPagination(Set<String> tags, Integer currentPage) {
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多选择 10 个标签");
        }
        // 格式化当前日期
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String dateStr = LocalDate.now().format(formatter);

        // 更新标签搜索次数
        RScoredSortedSet<String> searchHotSortedSet = redissonClient
                .getScoredSortedSet(RedisConstant.SEARCH_HOT_KEY + dateStr);
        searchHotSortedSet.expire(RedisConstant.SEARCH_HOT_TTL);
        tags.forEach(tag -> searchHotSortedSet.addScore(tag, 1));

        // 分页相关参数
        long total;
        List<Long> idRecords;
        int DEFAULT_PAGE_SIZE = 20;
        int start = (currentPage - 1) * DEFAULT_PAGE_SIZE;
        int end = currentPage * DEFAULT_PAGE_SIZE - 1;

        // 检查搜索结果是否已缓存
        String searchTagsKey = RedisConstant.SEARCH_TAGS_KEY + String.join(",", tags);
        RList<Long> searchTagsList = redissonClient.getList(searchTagsKey);
        int cacheSize = searchTagsList.size();
        if (cacheSize > 0) {
            total = cacheSize;
            idRecords = searchTagsList.range(start, end);
        } else {
            // 统计用户的标签匹配次数
            Map<Long, Integer> matchCount = new HashMap<>();
            tags.forEach(tag -> {
                RSet<Long> userIdSet = redissonClient.getSet(RedisConstant.TAGS_KEY + tag);
                userIdSet.forEach(userId -> matchCount.merge(userId, 1, Integer::sum));
            });
            // 按标签匹配次数降序排序
            List<Long> userIds = matchCount.keySet()
                    .stream()
                    .sorted((a, b) -> matchCount.get(b) - matchCount.get(a))
                    .collect(Collectors.toList());
            // 缓存搜索结果
            searchTagsList.addAll(userIds);
            searchTagsList.expire(RedisConstant.SEARCH_TAGS_TTL);
            // 获得分页数据
            total = userIds.size();
            idRecords = userIds.subList(start, Math.min(end + 1, userIds.size()));
        }
        // 查询分页后的用户信息
        List<User> userRecords = queryByIdsWithCache(idRecords);
        return new Page<User>(currentPage, DEFAULT_PAGE_SIZE, total).setRecords(userRecords);
    }


    /**
     * 根据 ID 批量查询用户信息
     */
    @Override
    public List<User> queryByIdsWithCache(List<Long> userIds) {
        Map<Long, User> userMap = new HashMap<>();

        // 将已缓存的用户信息添加到结果集
        String[] userIdsWithPrefix = userIds.stream()
                .map(userId -> RedisConstant.USER_INFO_KEY + userId)
                .toArray(String[]::new);
        Map<String, User> cachedMap = redissonClient.getBuckets()
                .get(userIdsWithPrefix);
        cachedMap.forEach((userIdWithPrefix, user) -> {
            Long userId = Long.valueOf(userIdWithPrefix.substring(RedisConstant.USER_INFO_KEY.length()));
            userMap.put(userId, user);
        });

        // 将未缓存的用户查询数据库并缓存
        List<Long> uncachedIds = userIds.stream()
                .filter(userId -> !cachedMap.containsKey(RedisConstant.USER_INFO_KEY + userId))
                .collect(Collectors.toList());
        if (!uncachedIds.isEmpty()) {
            this.lambdaQuery()
                    .in(User::getId, uncachedIds)
                    .list()
                    .forEach(user -> {
                        Long userId = user.getId();
                        UserVO userVO = getUserVO(user);
                        User userTemp = new User();
                        BeanUtils.copyProperties(userVO, userTemp);
                        user = userTemp;
                        redissonClient.getBucket(RedisConstant.USER_INFO_KEY + userId)
                                .set(user, RedisConstant.USER_INFO_TTL.toMillis(), TimeUnit.MILLISECONDS);
                        userMap.put(userId, user);
                    });
        }

        return userIds.stream().map(userMap::get).collect(Collectors.toList());
    }

    /**
     * 查询近期热门搜索标签
     */
    @Override
    public List<String> queryHotTags() {
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");

        // 统计近三天的热门标签
        Map<String, Double> tagToHotMap = new TreeMap<>();
        for (int i = 0; i < 3; i++) {
            int daysBefore = i;
            String dateBeforeStr = now.minusDays(daysBefore).format(formatter);
            RScoredSortedSet<String> searchHotSortedSet = redissonClient
                    .getScoredSortedSet(RedisConstant.SEARCH_HOT_KEY + dateBeforeStr);
            searchHotSortedSet.entryRangeReversed(0, 9)
                    .forEach(entry -> {
                        String tag = entry.getValue();
                        Double hot = entry.getScore() * (3 - daysBefore);
                        tagToHotMap.merge(tag, hot, Double::sum);
                    });
        }

        // 取最热门的 10 个标签
        List<String> tagHotList = tagToHotMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(10)
                .collect(Collectors.toList());
        return tagHotList;
    }

    /**
     * 分页推荐标签相近的用户
     */
    @Override
    public Page<User> recommendUsers(Long userId, Integer currentPage) {
        List<String> hotTags = queryHotTags();

        // 默认随机使用热门标签推荐用户
        Set<String> recommendTags = RandomUtil.randomEleSet(hotTags, Math.min(3, hotTags.size()));

        // 若用户已登录则按用户标签推荐
        Optional.ofNullable(userId)
                .map(id -> this.getById(userId))
                .map(User::getTags)
                .ifPresent(tags -> {
                    // 若用户标签不足 3 个则随机补充热门标签
                    if (tags.size() < 3)
                        tags.addAll(RandomUtil.randomEleSet(hotTags,
                                Math.min(3 - tags.size(), hotTags.size())));
                    recommendTags.clear();
                    recommendTags.addAll(tags);
                });

        return queryByTagsWithPagination(recommendTags, currentPage);
    }

    /**
     * 根据昵称分页查询用户
     */
    @Override
    public Page<UserVO> queryByUsernameWithPagination(String username, Integer currentPage) {
        // 匹配昵称或未设置昵称时的账号
        int DEFAULT_PAGE_SIZE = 20;

        // 创建 QueryWrapper 对象并设置查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("username", username)
                .or(w -> w.isNull("username")
                        .like("username", username));

        // 执行分页查询
        Page<User> userPage = this.page(new Page<>(currentPage, DEFAULT_PAGE_SIZE), queryWrapper);

        // 转换记录
        List<UserVO> userVOs = userPage.getRecords()
                .stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());

        // 创建新的 Page<UserVO> 对象并设置记录
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        userVOPage.setRecords(userVOs);

        return userVOPage;
    }

    /**
     * 向列表中的用户发送消息
     */
    @Override
    public void sendMessages(String content, Set<Long> userIds, HttpServletRequest request) {
        long scrollId = IdUtils.generateUniqueId();
        User loginUser = getLoginUser(request);
        Message message = new Message().setContent(content)
                .setIsUnread(true)
                .setSenderId(loginUser.getId())
                .setSendTime(LocalDateTime.now())
                .setScrollId(scrollId);

        for (Long userId : userIds) {
            RScoredSortedSet<Message> messageSortedSet = redissonClient.getScoredSortedSet(
                    RedisConstant.USER_MESSAGE_KEY + userId);
            messageSortedSet.add(scrollId, message);
        }
    }

    /**
     * 获取未读消息数量
     */
    @Override
    public Integer getUnreadMessageCount(HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        RScoredSortedSet<Message> messageSortedSet = redissonClient.getScoredSortedSet(
                RedisConstant.USER_MESSAGE_KEY + loginUser.getId());
        return (int) messageSortedSet.valueRangeReversed(0, 99)
                .stream()
                .filter(Message::getIsUnread)
                .count();
    }

    /**
     * 滚动查询消息列表
     */
    @Override
    public List<Message> getMessageWithScrolling(Long scrollId, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        // 获取用户消息列表
        RScoredSortedSet<Message> messageSortedSet = redissonClient.getScoredSortedSet(
                RedisConstant.USER_MESSAGE_KEY + loginUser.getId());
        int DEFAULT_PAGE_SIZE = 20;
        // 滚动查询消息
        Collection<Message> messages = messageSortedSet.valueRangeReversed(
                0, false, scrollId, false, 0, DEFAULT_PAGE_SIZE);
        // 返回原始消息
        List<Message> messageRes = copyToList(messages, Message.class);
        // 将未读消息标记为已读
        for (Message message : messages) {
            if (message.getIsUnread()) {
                messageSortedSet.remove(message);
                message.setIsUnread(false);
                messageSortedSet.add(message.getScrollId(), message);
            }
        }
        return messageRes;
    }

    /**
     * 从一个对象列表到另一个对象列表的属性复制
     *
     * @param collection
     * @param targetType
     * @param <T>
     * @return
     */
    public static <T> List<T> copyToList(Collection<?> collection, Class<T> targetType) {
        List<T> targetList = new ArrayList<>();
        for (Object source : collection) {
            try {
                T target = targetType.newInstance();
                BeanUtils.copyProperties(source, target);
                targetList.add(target);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Failed to copy properties", e);
            }
        }
        return targetList;
    }

}




