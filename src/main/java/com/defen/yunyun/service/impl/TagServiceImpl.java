package com.defen.yunyun.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.defen.yunyun.model.entity.Tag;
import com.defen.yunyun.service.TagService;
import com.defen.yunyun.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author defen
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2023-09-16 16:41:42
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




