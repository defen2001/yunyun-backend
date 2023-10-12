package com.defen.yunyun.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.defen.yunyun.annotation.AuthCheck;
import com.defen.yunyun.common.BaseResponse;
import com.defen.yunyun.common.DeleteRequest;
import com.defen.yunyun.common.ErrorCode;
import com.defen.yunyun.common.ResultUtils;
import com.defen.yunyun.constant.UserConstant;
import com.defen.yunyun.exception.BusinessException;
import com.defen.yunyun.model.dto.Message;
import com.defen.yunyun.model.dto.user.*;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.vo.LoginUserVO;
import com.defen.yunyun.model.vo.UserVO;
import com.defen.yunyun.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 用户接口
 *
 * @author defen
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<String> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        Long result = userService.userRegister(userAccount, userPassword, checkPassword);
        if (result == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "注册失败");
        }
        return ResultUtils.success("注册成功");
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<UserLogin> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserLogin userLogin = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(userLogin);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        boolean result = userService.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(true);
    }

    // endregion

    /**
     * 修改密码
     * @param userPasswordRequest
     * @return
     */
    @PutMapping("/password")
    public BaseResponse<String> updatePassword(@RequestBody UserPasswordRequest userPasswordRequest, HttpServletRequest request) {
        if (userPasswordRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数错误");
        }
        userService.updatePassword(userPasswordRequest, request);
        return ResultUtils.success("密码设置成功");
    }

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     * @return
     */
    @PutMapping("/update/my")
    public BaseResponse<String> updateMyUser(@RequestBody UserUpdateMyRequest userUpdateMyRequest,
                                              HttpServletRequest request) {
        if (userUpdateMyRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = userService.updateMyUser(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success("更新成功");
    }

    /**
     * 用户编辑标签
     *
     * @param tags
     * @return
     */
    @PutMapping("/add/tags")
    public BaseResponse<String> updateTags(@RequestBody List<String> tags ,HttpServletRequest request) {
        if (tags == null){
            tags = Collections.emptyList();
        }
        User loginUser = userService.getLoginUser(request);
        userService.updateTags(tags ,loginUser);
        return ResultUtils.success("标签保存成功");
    }

    /**
     * 查询指定用户的信息
     *
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    public BaseResponse<User> queryByUserId(@PathVariable Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要查询的用户");
        }
        return ResultUtils.success(userService.queryByUserId(userId));
    }

    /**
     * 根据标签分页查询用户
     *
     * @param tags
     * @param currentPage
     * @return
     */
    @GetMapping("/tags")
    public BaseResponse<Page<User>> queryByTagsWithPagination(@RequestParam Set<String> tags, Integer currentPage) {
        if (tags == null || tags.isEmpty())
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请选择要查询的标签");
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        return ResultUtils.success(userService.queryByTagsWithPagination(tags, currentPage));
    }

    /**
     * 查询近期热门搜索标签
     *
     * @return
     */
    @GetMapping("/tags/hot")
    public BaseResponse<List<String>> queryHotTags() {
        return ResultUtils.success(userService.queryHotTags());
    }

    /**
     * 分页推荐标签相近的用户
     *
     * @param userId
     * @param currentPage
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(Long userId, Integer currentPage) {
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        return ResultUtils.success(userService.recommendUsers(userId, currentPage));
    }

    /**
     * 根据昵称分页查询用户
     *
     * @param username
     * @param currentPage
     * @return
     */
    @GetMapping("/name")
    public BaseResponse<Page<UserVO>> queryByUsernameWithPagination(String username, Integer currentPage) {
        if (Strings.isBlank(username))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入要查询的昵称");
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        return ResultUtils.success(userService.queryByUsernameWithPagination(username, currentPage));
    }

    /**
     * 获取未读消息数量
     *
     * @return
     */
    @GetMapping("/message/unread")
    public BaseResponse<Integer> getUnreadMessageCount(HttpServletRequest request) {
        return ResultUtils.success(userService.getUnreadMessageCount(request));
    }

    /**
     * 滚动查询消息列表
     *
     * @param scrollId
     * @return
     */
    @GetMapping("/message")
    public BaseResponse<List<Message>> getMessageWithScrolling(Long scrollId, HttpServletRequest request) {
        if (scrollId == null)
            scrollId = Long.MAX_VALUE;
        return ResultUtils.success(userService.getMessageWithScrolling(scrollId, request));
    }

}
