package com.zzy.petclinic.storage;

public interface ImageStorageService {
  String store(String category, Long entityId, ValidatedImage image);

  StoredImage load(String objectKey);

  void delete(String objectKey);
}
