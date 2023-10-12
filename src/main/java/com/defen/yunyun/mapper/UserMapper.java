package com.defen.yunyun.mapper;

import com.defen.yunyun.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author defen
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2023-09-16 16:42:36
* @Entity com.defen.yunyun.model.entity.User
*/
@Mapper
public interface UserMapper extends BaseMapper<User> {

}




