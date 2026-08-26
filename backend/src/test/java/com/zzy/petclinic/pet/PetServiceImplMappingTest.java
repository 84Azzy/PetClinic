package com.zzy.petclinic.pet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PetServiceImplMappingTest {
  private PetMapper mapper;
  private PetServiceImpl service;

  @BeforeEach
  void setUp() {
    mapper = mock(PetMapper.class);
    service = new PetServiceImpl(mapper);
  }

  @Test
  void createMapsEveryRequestField() {
    PetRequest request = completeRequest();

    Pet result = service.create(request);

    assertPetMatches(result, request);
    verify(mapper).insert(result);
  }

  @Test
  void updateMapsEveryRequestField() {
    Pet existing = new Pet();
    existing.setId(9L);
    when(mapper.selectById(9L)).thenReturn(existing);
    PetRequest request = completeRequest();

    Pet result = service.update(9L, request);

    assertSame(existing, result);
    assertPetMatches(result, request);
    verify(mapper).updateById(existing);
  }

  private static PetRequest completeRequest() {
    return new PetRequest(
        2L,
        3L,
        "团团",
        "FEMALE",
        "垂耳兔",
        LocalDate.of(2024, 5, 8),
        "白色",
        "MC-009",
        "无",
        "https://example.test/pets/9.jpg");
  }

  private static void assertPetMatches(Pet pet, PetRequest request) {
    assertAll(
        () -> assertEquals(request.ownerId(), pet.getOwnerId(), "ownerId 未映射"),
        () -> assertEquals(request.typeId(), pet.getTypeId(), "typeId 未映射"),
        () -> assertEquals(request.name(), pet.getName(), "name 未映射"),
        () -> assertEquals(request.gender(), pet.getGender(), "gender 未映射"),
        () -> assertEquals(request.breed(), pet.getBreed(), "breed 未映射"),
        () -> assertEquals(request.birthDate(), pet.getBirthDate(), "birthDate 未映射"),
        () -> assertEquals(request.color(), pet.getColor(), "color 未映射"),
        () -> assertEquals(request.microchipNo(), pet.getMicrochipNo(), "microchipNo 未映射"),
        () -> assertEquals(request.allergies(), pet.getAllergies(), "allergies 未映射"),
        () -> assertEquals(request.photoUrl(), pet.getPhotoUrl(), "photoUrl 未映射"));
  }
}
