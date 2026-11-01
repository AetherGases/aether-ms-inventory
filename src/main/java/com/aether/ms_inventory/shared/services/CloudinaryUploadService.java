package com.aether.ms_inventory.shared.services;

import com.aether.ms_inventory.shared.enums.StorageFileTypeEnum;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryUploadService {
  private final Cloudinary cloudinary;

  public CloudinaryUploadResult upload(byte[] file, String fileName, StorageFileTypeEnum fileType) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> result = cloudinary.uploader().upload(file, ObjectUtils.asMap(
          "folder", fileType.getCloudinaryFolder(),
          "resource_type", fileType.getCloudinaryResourceType(),
          "public_id", fileName
      ));

      return new CloudinaryUploadResult(
          (String) result.get("public_id"),
          (String) result.get("secure_url")
      );
    } catch (IOException e) {
      throw new IllegalStateException("Falha ao enviar o arquivo para o Cloudinary", e);
    }
  }

  public record CloudinaryUploadResult(
      String publicId,
      String secureUrl
  ) {
  }
}
