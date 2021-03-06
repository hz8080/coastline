package com.htc.coastline.mapper;

import com.htc.coastline.entity.AreaDTO;
import com.htc.coastline.entity.AreaQTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Created by zack.huang on 2018/4/16
 */
@Mapper
public interface AreaMapper {
    List<AreaDTO> select(AreaQTO areaQTO);

    void save(AreaDTO areaDTO);

    AreaDTO getAreaByImgName(String imgName);

    void update(AreaDTO areaDTO);

    int deleteAreaById(Long areaId);
}
