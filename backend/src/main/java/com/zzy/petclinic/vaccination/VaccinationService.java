package com.zzy.petclinic.vaccination;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.*;
import com.zzy.petclinic.pet.PetMapper;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VaccinationService {
  private final VaccinationMapper mapper;
  private final PetMapper pets;

  public PageResponse<VaccinationRecord> page(PageQuery q, Long petId, Boolean overdue) {
    LambdaQueryWrapper<VaccinationRecord> w =
        new LambdaQueryWrapper<VaccinationRecord>()
            .orderByDesc(VaccinationRecord::getVaccinatedDate);
    if (petId != null) w.eq(VaccinationRecord::getPetId, petId);
    if (Boolean.TRUE.equals(overdue))
      w.lt(VaccinationRecord::getNextDueDate, LocalDate.now())
          .ne(VaccinationRecord::getStatus, "COMPLETED");
    return PageResponse.of(mapper.selectPage(Page.of(q.pageValue(), q.sizeValue()), w));
  }

  public VaccinationRecord get(Long id) {
    VaccinationRecord x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound("疫苗记录");
    return x;
  }

  @Transactional
  public VaccinationRecord create(VaccinationRequest r) {
    if (pets.selectById(r.petId()) == null) throw BusinessException.notFound("宠物");
    VaccinationRecord x = new VaccinationRecord();
    apply(x, r);
    x.setStatus(
        r.nextDueDate() != null && r.nextDueDate().isBefore(LocalDate.now()) ? "OVERDUE" : "VALID");
    x.setCreatedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.insert(x);
    return x;
  }

  @Transactional
  public VaccinationRecord update(Long id, VaccinationRequest r) {
    VaccinationRecord x = get(id);
    apply(x, r);
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  public void delete(Long id) {
    mapper.deleteById(get(id));
  }

  private void apply(VaccinationRecord x, VaccinationRequest r) {
    x.setPetId(r.petId());
    x.setVaccineName(r.vaccineName());
    x.setBatchNo(r.batchNo());
    x.setVaccinatedDate(r.vaccinatedDate());
    x.setNextDueDate(r.nextDueDate());
    x.setVeterinarian(r.veterinarian());
    x.setNotes(r.notes());
  }
}
