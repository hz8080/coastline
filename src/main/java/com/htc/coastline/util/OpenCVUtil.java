package com.htc.coastline.util;

import com.htc.coastline.constant.Directory;
import com.htc.coastline.entity.BinValue;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class OpenCVUtil {
    private static Logger log = LoggerFactory.getLogger(OpenCVUtil.class);
    private final static int  WHITE_BIN = 255;
    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            for (Directory directory : Directory.values()) {
                String path = getFilePath(directory.getValue(), "");
                File file = new File(path);
                if (!file.exists()) {
                    file.mkdirs();
                }
            }
            log.info("初始化图片目录完成");
        } catch (Exception e){
            log.error("初始化图片目录异常", e);
        }
    }

    private static final int ratio = 3;

    public static void processImg(String fileName) {
        try {
            String filePath = getFilePath(Directory.ORIGIN_DIRECTORY.getValue(), fileName);
            Mat img = Imgcodecs.imread(filePath);
            Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
//            // 中值滤波
//            Imgproc.medianBlur(img, img, 9);
            // 高斯模糊
            Imgproc.GaussianBlur(img, img, new Size(21, 21), 0);
            // 输出灰度图
            String filePathGray = getFilePath(Directory.GRAY_DIRECTORY.getValue(), fileName);
            Imgcodecs.imwrite(filePathGray, img);
            // 灰度
            Mat matGrey = new Mat();
            MatOfInt histSize = new MatOfInt(256);
            MatOfFloat histRange = new MatOfFloat(0, 256);
            MatOfInt channels = new MatOfInt(0);
            Imgproc.calcHist(Collections.singletonList(img), channels, new Mat(), matGrey, histSize, histRange, false);
            Core.normalize(matGrey, matGrey, 0, matGrey.rows(), Core.NORM_MINMAX, -1, new Mat());
            // 灰度排序
            List<BinValue> binValueList = new ArrayList<>(256);
            for (int i = 0; i < histSize.get(0, 0)[0]; i++) {
                log.info(String.format("bin/value[%d]:", i) + matGrey.get(i, 0)[0]);
                binValueList.add(new BinValue(i, (int)matGrey.get(i, 0)[0]));
            }
            binValueList.sort(Comparator.comparing(BinValue::getValue));
            log.info("1st:" + binValueList.get(255).getBin() + "/" + binValueList.get(255).getValue());
            log.info("2nd:" + binValueList.get(254).getBin() + "/" + binValueList.get(254).getValue());
            log.info("3rd:" + binValueList.get(253).getBin() + "/" + binValueList.get(253).getValue());
            // 二值化
            Imgproc.threshold(img, img, 55, 255, Imgproc.THRESH_BINARY);
            String filePathThreshold = getFilePath(Directory.THRESHOLD_DIRECTORY.getValue(), fileName);
            Imgcodecs.imwrite(filePathThreshold, img);
            // 边缘检测
            int threshold = 100;
            Imgproc.Canny(img, img, threshold / ratio, threshold);
            String filePathEdge = getFilePath(Directory.EDGE_DIRECTORY.getValue(), fileName);
            Imgcodecs.imwrite(filePathEdge, img);
        } catch (Exception e) {
            log.error("发生异常", e);
        }
    }

    public static void main(String[] args) {
        processImg("2018_05_07_00_25_13.jpg");
    }

    public static String getFilePath(String directory, String fileName) {
        try {
            return ResourceUtils.getFile(directory).getAbsolutePath().concat("/").concat(fileName);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static void generateBlurImg(int ksize, String imgName) {
        String originFilePath = getFilePath(Directory.ORIGIN_DIRECTORY.getValue(), imgName);
        if (originFilePath != null){
            Mat img = Imgcodecs.imread(originFilePath);
            // 灰度化
            Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(img, img, new Size(ksize, ksize), 0);
            String filePathGray = getFilePath(Directory.GRAY_DIRECTORY.getValue(), imgName);
            Imgcodecs.imwrite(filePathGray, img);
        }
    }

    public static List<BinValue> getCharts(String imgName) {
        try {
            String grayImgPath = getFilePath(Directory.GRAY_DIRECTORY.getValue(), imgName);
            if (grayImgPath != null) {
                Mat img = Imgcodecs.imread(grayImgPath);
                Mat matGrey = new Mat();
                MatOfInt histSize = new MatOfInt(256);
                MatOfFloat histRange = new MatOfFloat(0, 256);
                MatOfInt channels = new MatOfInt(0);
                Imgproc.calcHist(Collections.singletonList(img), channels, new Mat(), matGrey, histSize, histRange, false);

                // 归一化，令数值按比例缩小
                Core.normalize(matGrey, matGrey, 0, matGrey.rows(), Core.NORM_MINMAX, -1, new Mat());

                // 放入list
                List<BinValue> binValueList = new ArrayList<>(256);
                for (int i = 0; i < histSize.get(0, 0)[0]; i++) {
//                    log.info(String.format("bin/value[%d]:", i) + matGrey.get(i, 0)[0]);
                    binValueList.add(new BinValue(i, (int)matGrey.get(i, 0)[0]));
                }

                return binValueList;
            }
            return null;
        }catch (Exception e){
            log.error("获取灰度值发生异常", e);
            return null;
        }
    }

    public static void generateThresholdImg(int threshold, String imgName){
        try {
            String grayImgPath = getFilePath(Directory.GRAY_DIRECTORY.getValue(), imgName);
            String thresholdImgPath = getFilePath(Directory.THRESHOLD_DIRECTORY.getValue(), imgName);
            if (grayImgPath != null) {
                Mat img = Imgcodecs.imread(grayImgPath);
                Imgproc.threshold(img, img, threshold, 255, Imgproc.THRESH_BINARY);
                Imgcodecs.imwrite(thresholdImgPath, img);
                log.info("边缘检测完成");
            }else
                log.error("thresholdImg path is null");
        }catch (Exception e){
            log.error("二值化发生异常", e);
        }
    }

    public static void generateEdgeImg(int threshold, String imgName) {
        try {
            String thresholdImgPath = getFilePath(Directory.THRESHOLD_DIRECTORY.getValue(), imgName);
            if (thresholdImgPath !=null){
                Mat img = Imgcodecs.imread(thresholdImgPath, Imgcodecs.IMREAD_GRAYSCALE);
                Imgproc.Canny(img, img, threshold / ratio, threshold);
                String filePathEdge = getFilePath(Directory.EDGE_DIRECTORY.getValue(), imgName);
                Imgcodecs.imwrite(filePathEdge, img);
                log.info("边缘检测完成");
            }
        }catch (Exception e){
            log.error("边缘检测发生异常", e);
        }
    }

    public static double getEdgePixelNum(String imgName){
        try {
            String edgeImgPath = getFilePath(Directory.EDGE_DIRECTORY.getValue(), imgName);
            if (edgeImgPath != null) {
                Mat img = Imgcodecs.imread(edgeImgPath, Imgcodecs.IMREAD_GRAYSCALE);

                Mat matGrey = new Mat();
                MatOfInt histSize = new MatOfInt(256);
                MatOfFloat histRange = new MatOfFloat(0, 256);
                MatOfInt channels = new MatOfInt(0);
                Imgproc.calcHist(Collections.singletonList(img), channels, new Mat(), matGrey, histSize, histRange, false);

//                for (int i = 0; i < histSize.get(0, 0)[0]; i++) {
//                    log.info(String.format("bin/value[%d]:", i) + matGrey.get(i, 0)[0]);
//                }
                return matGrey.get(WHITE_BIN, 0)[0];
            }else {
                log.error("imgPath is null");
                return 0.0;
            }
        }catch (Exception e){
            log.error("检测边缘长度异常");
            return 0.0;
        }
    }
}