package com.defen.yunyun.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.defen.yunyun.model.dto.Message;
import com.defen.yunyun.model.dto.user.UserLogin;
import com.defen.yunyun.model.dto.user.UserPasswordRequest;
import com.defen.yunyun.model.dto.user.UserQueryRequest;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.vo.LoginUserVO;
import com.defen.yunyun.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * 用户服务
 * @author defen
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    UserLogin userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);


    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    void updatePassword(UserPasswordRequest userPasswordRequest, HttpServletRequest request);

    boolean updateMyUser(User user);

    void updateTags(List<String> tags, User request);

    User queryByUserId(Long userId);

    Page<User> queryByTagsWithPagination(Set<String> tags, Integer currentPage);

    List<User> queryByIdsWithCache(List<Long> userIds);

    List<String> queryHotTags();

    Page<User> recommendUsers(Long userId, Integer currentPage);

    Page<UserVO> queryByUsernameWithPagination(String username, Integer currentPage);

    void sendMessages(String content, Set<Long> userIds, HttpServletRequest request);

    Integer getUnreadMessageCount(HttpServletRequest request);

    List<Message> getMessageWithScrolling(Long scrollId, HttpServletRequest request);

}
