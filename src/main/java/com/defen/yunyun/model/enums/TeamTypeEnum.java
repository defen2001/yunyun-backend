package com.defen.yunyun.model.enums;

/**
 * 队伍状态枚举
 *
 */
public enum TeamTypeEnum {

    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密");

    private int value;

    private String text;


    public static TeamTypeEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        TeamTypeEnum[] values = TeamTypeEnum.values();
        for (TeamTypeEnum teamTypeEnum : values) {
            if (teamTypeEnum.getValue() == value) {
                return teamTypeEnum;
            }
        }
        return null;
    }

    TeamTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
