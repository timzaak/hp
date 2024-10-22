package com.timzaak.backend.mapper;

import org.apache.ibatis.annotations.Select;

public interface TestDBMapper {

    @Select("select 1=1")
    Boolean check();
}
