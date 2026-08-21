package com.zzy.petclinic.vet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class VetService {
  private final VetMapper mapper;
  private final VetSpecialtyMapper relationMapper;

  public PageResponse<Vet> page(PageQuery q, Long specialtyId) {
    LambdaQueryWrapper<Vet> w = new LambdaQueryWrapper<Vet>().orderByDesc(Vet::getId);
    if (StringUtils.hasText(q.keyword())) w.like(Vet::getName, q.keyword());
    if (StringUtils.hasText(q.status())) w.eq(Vet::getStatus, q.status());
    if (specialtyId != null) {
      List<Long> ids =
          relationMapper
              .selectList(
                  new LambdaQueryWrapper<VetSpecialty>()
                      .eq(VetSpecialty::getSpecialtyId, specialtyId))
              .stream()
              .map(VetSpecialty::getVetId)
              .toList();
      if (ids.isEmpty()) return new PageResponse<>(List.of(), 0, q.pageValue(), q.sizeValue());
      w.in(Vet::getId, ids);
    }
    return PageResponse.of(mapper.selectPage(Page.of(q.pageValue(), q.sizeValue()), w));
  }

  public Vet get(Long id) {
    Vet x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound("兽医");
    return x;
  }

  public List<Long> specialties(Long id) {
    get(id);
    return relationMapper
        .selectList(new LambdaQueryWrapper<VetSpecialty>().eq(VetSpecialty::getVetId, id))
        .stream()
        .map(VetSpecialty::getSpecialtyId)
        .toList();
  }

  @Transactional
  public Vet create(VetRequest r) {
    Vet x = new Vet();
    apply(x, r);
    x.setCreatedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.insert(x);
    replaceSpecialties(x.getId(), r.specialtyIds());
    return x;
  }

  @Transactional
  public Vet update(Long id, VetRequest r) {
    Vet x = get(id);
    apply(x, r);
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    replaceSpecialties(id, r.specialtyIds());
    return x;
  }

  @Transactional
  public void disable(Long id) {
    Vet x = get(id);
    x.setStatus("INACTIVE");
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
  }

  private void replaceSpecialties(Long id, List<Long> ids) {
    relationMapper.delete(new LambdaQueryWrapper<VetSpecialty>().eq(VetSpecialty::getVetId, id));
    if (ids != null)
      ids.stream()
          .distinct()
          .forEach(
              s -> {
                VetSpecialty r = new VetSpecialty();
                r.setVetId(id);
                r.setSpecialtyId(s);
                relationMapper.insert(r);
              });
  }

  private void apply(Vet x, VetRequest r) {
    x.setName(r.name());
    x.setPhone(r.phone());
    x.setEmail(r.email());
    x.setLicenseNo(r.licenseNo());
    x.setBiography(r.biography());
    x.setAvatarUrl(r.avatarUrl());
    x.setStatus(r.status() == null ? "ACTIVE" : r.status());
  }
}
