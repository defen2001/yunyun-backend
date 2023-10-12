package com.defen.yunyun.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户密码请求体
 *
 * @author defen
 */
@Data
public class UserPasswordRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String password;

    private String oldPassword;

    private String confirmPassword;
}
