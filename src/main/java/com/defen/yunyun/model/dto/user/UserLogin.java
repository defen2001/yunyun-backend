package com.defen.yunyun.model.dto.user;

import com.defen.yunyun.model.vo.LoginUserVO;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应信息
 */
@Data
@AllArgsConstructor
public class UserLogin {
    private LoginUserVO user;
    private String token;
}
