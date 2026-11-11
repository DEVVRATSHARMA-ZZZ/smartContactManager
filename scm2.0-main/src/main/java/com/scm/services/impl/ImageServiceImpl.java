package com.scm.services.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.scm.helpers.AppConstants;
import com.scm.services.ImageService;

@Service
public class ImageServiceImpl implements ImageService {

    private Cloudinary cloudinary;

    public ImageServiceImpl(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @Override
    public String uploadImage(MultipartFile contactImage, String filename) {

        try {
            byte[] data = contactImage.getBytes();

            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    data,
                    ObjectUtils.asMap(
                            "public_id", filename,
                            "overwrite", true,
                            "resource_type", "image")
            );

            Object secureUrl = uploadResult.get("secure_url");
            if (secureUrl != null && !secureUrl.toString().isBlank()) {
                return secureUrl.toString();
            }

            return this.getUrlFromPublicId(filename);

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image", e);
        }
    }


    @Override
    public String getUrlFromPublicId(String publicId) {

        return cloudinary
                .url()
                .transformation(
                        new Transformation<>()
                                .width(AppConstants.CONTACT_IMAGE_WIDTH)
                                .height(AppConstants.CONTACT_IMAGE_HEIGHT)
                                .crop(AppConstants.CONTACT_IMAGE_CROP))
                .generate(publicId);

    }

}
