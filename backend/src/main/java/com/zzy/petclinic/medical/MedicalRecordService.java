package com.zzy.petclinic.medical;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.*;
import com.zzy.petclinic.visit.*;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {
  private final MedicalRecordMapper mapper;
  private final VisitMapper visits;

  public PageResponse<MedicalRecord> page(PageQuery q, Long petId) {
    LambdaQueryWrapper<MedicalRecord> w =
        new LambdaQueryWrapper<MedicalRecord>().orderByDesc(MedicalRecord::getId);
    if (petId != null) w.eq(MedicalRecord::getPetId, petId);
    return PageResponse.of(mapper.selectPage(Page.of(q.pageValue(), q.sizeValue()), w));
  }

  public MedicalRecord get(Long id) {
    MedicalRecord x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound("病历");
    return x;
  }

  @Transactional
  public MedicalRecord create(MedicalRecordRequest r) {
    if (mapper.selectCount(
            new LambdaQueryWrapper<MedicalRecord>().eq(MedicalRecord::getVisitId, r.visitId()))
        > 0) throw BusinessException.conflict("该就诊已建立病历");
    Visit v = visits.selectById(r.visitId());
    if (v == null) throw BusinessException.notFound("就诊记录");
    MedicalRecord x = new MedicalRecord();
    x.setVisitId(v.getId());
    x.setPetId(v.getPetId());
    x.setVetId(v.getVetId());
    apply(x, r);
    x.setCreatedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.insert(x);
    return x;
  }

  @Transactional
  public MedicalRecord update(Long id, MedicalRecordRequest r) {
    MedicalRecord x = get(id);
    apply(x, r);
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  public void delete(Long id) {
    mapper.deleteById(get(id));
  }

  private void apply(MedicalRecord x, MedicalRecordRequest r) {
    x.setSymptoms(r.symptoms());
    x.setDiagnosis(r.diagnosis());
    x.setTreatment(r.treatment());
    x.setPrescription(r.prescription());
    x.setNotes(r.notes());
  }
}
