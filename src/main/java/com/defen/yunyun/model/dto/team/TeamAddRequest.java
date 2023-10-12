package com.defen.yunyun.model.dto.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建队伍请求体
 */
@Data
public class TeamAddRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer memberLimit;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date expireTime;

    /**
     * 用户id
     */
    private Long leaderId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer type;

    /**
     * 密码
     */
    private String password;
}
