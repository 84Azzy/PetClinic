package com.zzy.petclinic.catalog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zzy.petclinic.common.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
  private final PetTypeMapper petTypes;
  private final SpecialtyMapper specialties;

  public CatalogService(PetTypeMapper p, SpecialtyMapper s) {
    petTypes = p;
    specialties = s;
  }

  @PreAuthorize("isAuthenticated()")
  public List<PetType> petTypes() {
    return petTypes.selectList(new QueryWrapper<PetType>().orderByAsc("name"));
  }

  @PreAuthorize("isAuthenticated()")
  public List<Specialty> specialties() {
    return specialties.selectList(new QueryWrapper<Specialty>().orderByAsc("name"));
  }

  @PreAuthorize("isAuthenticated()")
  public PetType petType(Long id) {
    return required(petTypes, id, "宠物类型");
  }

  @PreAuthorize("isAuthenticated()")
  public Specialty specialty(Long id) {
    return required(specialties, id, "专长");
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public PetType createPetType(CatalogRequest r) {
    PetType x = new PetType();
    apply(x, r);
    petTypes.insert(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public Specialty createSpecialty(CatalogRequest r) {
    Specialty x = new Specialty();
    apply(x, r);
    specialties.insert(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public PetType updatePetType(Long id, CatalogRequest r) {
    PetType x = petType(id);
    apply(x, r);
    petTypes.updateById(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public Specialty updateSpecialty(Long id, CatalogRequest r) {
    Specialty x = specialty(id);
    apply(x, r);
    specialties.updateById(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public void deletePetType(Long id) {
    petTypes.deleteById(petType(id));
  }

  @Transactional
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public void deleteSpecialty(Long id) {
    specialties.deleteById(specialty(id));
  }

  private <T> T required(BaseMapper<T> mapper, Long id, String label) {
    T x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound(label);
    return x;
  }

  private void apply(PetType x, CatalogRequest r) {
    x.setName(r.name());
    x.setDescription(r.description());
    x.setStatus(r.status() == null ? "ACTIVE" : r.status());
    x.setUpdatedAt(LocalDateTime.now());
    if (x.getCreatedAt() == null) x.setCreatedAt(LocalDateTime.now());
  }

  private void apply(Specialty x, CatalogRequest r) {
    x.setName(r.name());
    x.setDescription(r.description());
    x.setStatus(r.status() == null ? "ACTIVE" : r.status());
    x.setUpdatedAt(LocalDateTime.now());
    if (x.getCreatedAt() == null) x.setCreatedAt(LocalDateTime.now());
  }
}
