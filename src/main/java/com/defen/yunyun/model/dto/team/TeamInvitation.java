package com.defen.yunyun.model.dto.team;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
public class TeamInvitation implements Serializable {
    /**
     * 队伍 ID
     */
    private Long teamId;
    /**
     * 邀请者 ID
     */
    private Long inviter;
    /**
     * 受邀者 ID
     */
    private Long invitee;
    /**
     * 邀请码
     */
    private String invitationCode;
}
