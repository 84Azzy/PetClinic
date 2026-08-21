package com.zzy.petclinic.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.*;
import com.zzy.petclinic.vet.VetService;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {
  private final VetScheduleSlotMapper mapper;
  private final VetService vets;

  public PageResponse<VetScheduleSlot> page(
      PageQuery q, Long vetId, LocalDate date, String status) {
    LambdaQueryWrapper<VetScheduleSlot> w =
        new LambdaQueryWrapper<VetScheduleSlot>().orderByAsc(VetScheduleSlot::getStartTime);
    if (vetId != null) w.eq(VetScheduleSlot::getVetId, vetId);
    if (date != null)
      w.ge(VetScheduleSlot::getStartTime, date.atStartOfDay())
          .lt(VetScheduleSlot::getStartTime, date.plusDays(1).atStartOfDay());
    if (status != null) w.eq(VetScheduleSlot::getStatus, status);
    return PageResponse.of(mapper.selectPage(Page.of(q.pageValue(), q.sizeValue()), w));
  }

  public List<VetScheduleSlot> available(Long vetId, LocalDate date) {
    return mapper.selectList(
        new LambdaQueryWrapper<VetScheduleSlot>()
            .eq(VetScheduleSlot::getVetId, vetId)
            .eq(VetScheduleSlot::getStatus, "AVAILABLE")
            .ge(
                date != null,
                VetScheduleSlot::getStartTime,
                date == null ? null : date.atStartOfDay())
            .lt(
                date != null,
                VetScheduleSlot::getStartTime,
                date == null ? null : date.plusDays(1).atStartOfDay())
            .orderByAsc(VetScheduleSlot::getStartTime));
  }

  public VetScheduleSlot get(Long id) {
    VetScheduleSlot x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound("排班时段");
    return x;
  }

  @Transactional
  public VetScheduleSlot create(SlotRequest r) {
    vets.get(r.vetId());
    if (!r.endTime().isAfter(r.startTime()))
      throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
    ensureFree(r.vetId(), r.startTime());
    VetScheduleSlot x = new VetScheduleSlot();
    x.setVetId(r.vetId());
    x.setStartTime(r.startTime());
    x.setEndTime(r.endTime());
    x.setStatus("AVAILABLE");
    x.setNote(r.note());
    x.setCreatedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.insert(x);
    return x;
  }

  @Transactional
  public VetScheduleSlot update(Long id, SlotRequest r) {
    VetScheduleSlot x = get(id);
    if ("BOOKED".equals(x.getStatus())) throw BusinessException.conflict("已预约时段不能修改");
    if (!r.endTime().isAfter(r.startTime()))
      throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
    x.setVetId(r.vetId());
    x.setStartTime(r.startTime());
    x.setEndTime(r.endTime());
    x.setNote(r.note());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  public int batch(Long vetId, BatchSlotRequest r) {
    vets.get(vetId);
    if (r.endDate().isBefore(r.startDate()) || !r.dailyEnd().isAfter(r.dailyStart()))
      throw new BusinessException(org.springframework.http.HttpStatus.BAD_REQUEST, "日期或每日时间范围不正确");
    int count = 0;
    for (LocalDate d = r.startDate(); !d.isAfter(r.endDate()); d = d.plusDays(1)) {
      for (LocalDateTime t = LocalDateTime.of(d, r.dailyStart());
          !t.plusMinutes(r.intervalMinutes()).isAfter(LocalDateTime.of(d, r.dailyEnd()));
          t = t.plusMinutes(r.intervalMinutes())) {
        if (mapper.selectCount(
                new LambdaQueryWrapper<VetScheduleSlot>()
                    .eq(VetScheduleSlot::getVetId, vetId)
                    .eq(VetScheduleSlot::getStartTime, t))
            == 0) {
          create(new SlotRequest(vetId, t, t.plusMinutes(r.intervalMinutes()), r.note()));
          count++;
        }
      }
    }
    return count;
  }

  @Transactional
  public void close(Long id) {
    VetScheduleSlot x = get(id);
    if ("BOOKED".equals(x.getStatus())) throw BusinessException.conflict("已预约时段不能关闭");
    x.setStatus("CLOSED");
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
  }

  @Transactional
  public void delete(Long id) {
    VetScheduleSlot x = get(id);
    if ("BOOKED".equals(x.getStatus())) throw BusinessException.conflict("已预约时段不能删除");
    mapper.deleteById(id);
  }

  private void ensureFree(Long vetId, LocalDateTime start) {
    if (mapper.selectCount(
            new LambdaQueryWrapper<VetScheduleSlot>()
                .eq(VetScheduleSlot::getVetId, vetId)
                .eq(VetScheduleSlot::getStartTime, start))
        > 0) throw BusinessException.conflict("该兽医的开始时间已存在排班");
  }
}
