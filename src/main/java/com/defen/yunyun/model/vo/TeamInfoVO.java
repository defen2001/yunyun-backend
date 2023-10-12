package com.defen.yunyun.model.vo;

import com.defen.yunyun.model.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 队伍信息视图
 */
@Data
public class TeamInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private User leader;
    private Integer type;
    private Boolean hasPassword;
    private Integer memberCount;
    private Integer memberLimit;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
