package com.defen.yunyun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defen.yunyun.mapper.UserMapper;
import com.defen.yunyun.model.dto.user.UserQueryRequest;
import com.defen.yunyun.model.entity.User;
import com.defen.yunyun.model.vo.LoginUserVO;
import com.defen.yunyun.model.vo.UserVO;
import com.defen.yunyun.service.UserService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户服务实现类
 *
 * @author defen
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        return 0;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        return null;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        return null;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        return false;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        return false;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return null;
    }

    @Override
    public UserVO getUserVO(User user) {
        return null;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        return null;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return null;
    }
}




