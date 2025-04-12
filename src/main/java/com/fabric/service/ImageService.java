package com.fabric.service;

import com.fabric.database.entity.Clothing;
import com.fabric.database.entity.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    void saveAll(List<Image> extractedImages);

    void deleteAll(List<String> publicIds);

    void saveImageInCloud(MultipartFile file, Clothing clothing, String side);

    void saveImageInHost(MultipartFile file, Clothing clothing, String side);

    String getUniqueImageId(Clothing clothing, String side);

    Image findByPublicId(String path);

    void deleteImage(Image image);
}
