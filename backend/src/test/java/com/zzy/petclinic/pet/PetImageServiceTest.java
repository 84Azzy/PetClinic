package com.zzy.petclinic.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.storage.ImageStorageService;
import com.zzy.petclinic.storage.ImageUploadValidator;
import com.zzy.petclinic.storage.ValidatedImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PetImageServiceTest {
  private PetMapper mapper;
  private ImageStorageService storage;
  private ImageUploadValidator validator;
  private PetImageService service;

  @BeforeEach
  void setUp() {
    mapper = mock(PetMapper.class);
    storage = mock(ImageStorageService.class);
    validator = mock(ImageUploadValidator.class);
    service = new PetImageService(mapper, mock(PetService.class), storage, validator);
  }

  @Test
  void replacingPhotoUpdatesDatabaseThenRemovesOldObject() {
    Pet pet = new Pet();
    pet.setId(7L);
    pet.setPhotoObjectKey("petclinic/dev/pets/7/old.jpg");
    when(mapper.selectById(7L)).thenReturn(pet);
    ValidatedImage image = new ValidatedImage(new byte[] {1}, "image/jpeg", "jpg");
    when(validator.validate(any())).thenReturn(image);
    when(storage.store("pets", 7L, image)).thenReturn("petclinic/dev/pets/7/new.jpg");

    Pet result =
        service.replace(7L, new MockMultipartFile("file", "pet.jpg", "image/jpeg", new byte[] {1}));

    assertEquals("petclinic/dev/pets/7/new.jpg", result.getPhotoObjectKey());
    verify(mapper).updateById(pet);
    verify(storage).delete("petclinic/dev/pets/7/old.jpg");
  }

  @Test
  void databaseFailureCleansUpNewObject() {
    Pet pet = new Pet();
    pet.setId(7L);
    when(mapper.selectById(7L)).thenReturn(pet);
    ValidatedImage image = new ValidatedImage(new byte[] {1}, "image/jpeg", "jpg");
    when(validator.validate(any())).thenReturn(image);
    when(storage.store("pets", 7L, image)).thenReturn("petclinic/dev/pets/7/new.jpg");
    when(mapper.updateById(pet)).thenThrow(new IllegalStateException("database unavailable"));

    assertThrows(
        IllegalStateException.class,
        () ->
            service.replace(
                7L,
                new MockMultipartFile("file", "pet.jpg", "image/jpeg", new byte[] {1})));
    verify(storage).delete("petclinic/dev/pets/7/new.jpg");
  }

  @Test
  void deletingPhotoExplicitlyClearsNullableObjectKey() {
    Pet pet = new Pet();
    pet.setId(7L);
    pet.setPhotoObjectKey("petclinic/dev/pets/7/old.jpg");
    when(mapper.selectById(7L)).thenReturn(pet);

    service.delete(7L);

    verify(mapper).update(isNull(), any());
    verify(storage).delete("petclinic/dev/pets/7/old.jpg");
  }
}
