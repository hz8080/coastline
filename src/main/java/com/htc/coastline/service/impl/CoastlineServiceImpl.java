package com.htc.coastline.service.impl;

import com.htc.coastline.constant.Directory;
import com.htc.coastline.entity.AreaDTO;
import com.htc.coastline.entity.AreaQTO;
import com.htc.coastline.entity.BinValue;
import com.htc.coastline.entity.BinValueDoubleList;
import com.htc.coastline.mapper.AreaMapper;
import com.htc.coastline.service.CoastlineService;
import com.htc.coastline.util.FileNameUtil;
import com.htc.coastline.util.OpenCVUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by zack.huang on 2018/4/16
 */
@Service
public class CoastlineServiceImpl implements CoastlineService {
    @Resource
    private AreaMapper areaMapper;

    @Override
    public List<AreaDTO> selectAreas(AreaQTO areaQTO) {
        return areaMapper.select(areaQTO);
    }

    @Override
    public String upload(String areaName, int resolution, int coastlineType, MultipartFile file, Date imgTime) {
        try {
            String fileSuffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = FileNameUtil.getFileName().concat(fileSuffix);
            String filePath = OpenCVUtil.getFilePath(Directory.ORIGIN_DIRECTORY.getValue(), fileName);
            File originFile = new File(filePath);
            if (!originFile.exists()) {
                originFile.createNewFile();
            }

            file.transferTo(originFile);
            // 存db
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setImgName(fileName);
            areaDTO.setCoastlineLength(0);
            areaDTO.setResolution(resolution);
            areaDTO.setCoastlineType(coastlineType);
            areaDTO.setAreaName(areaName);
            areaDTO.setImgTime(imgTime);
            areaMapper.save(areaDTO);

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void generateBlurImg(int ksize, String imgName) {
        OpenCVUtil.generateBlurImg(ksize, imgName);
    }

    @Override
    public BinValueDoubleList getCharts(String imgName) {
        List<BinValue> binValueList = OpenCVUtil.getCharts(imgName);
        if (binValueList == null) {
            return null;
        }
        BinValueDoubleList binValueDoubleList = new BinValueDoubleList();
        binValueDoubleList.setBinList(binValueList.stream().map(BinValue::getBin).collect(Collectors.toList()));
        binValueDoubleList.setValueList(binValueList.stream().map(BinValue::getValue).collect(Collectors.toList()));
        return binValueDoubleList;
    }

    @Override
    public void generateThresholdImg(int threshold, String imgName) {
        OpenCVUtil.generateThresholdImg(threshold, imgName);
    }

    @Override
    public void generateEdgeImg(int cannyThreshold, int threshold, String imgName) {
        OpenCVUtil.generateEdgeImg(cannyThreshold, threshold, imgName);
    }

    @Override
    public int getCoastlineLength(int cannyThreshold, int threshold, String imgName) {
        AreaDTO areaDTO = areaMapper.getAreaByImgName(imgName);
        // calculate
        double pixelNum = OpenCVUtil.getEdgePixelNum(cannyThreshold, threshold, imgName);
        int length = (int) (areaDTO.getResolution() * pixelNum);
        areaDTO.setCoastlineLength(length);
        // update db
        areaMapper.update(areaDTO);

        return length;
    }

}
