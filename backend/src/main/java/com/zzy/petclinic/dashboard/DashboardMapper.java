package com.zzy.petclinic.dashboard;

import java.util.*;
import org.apache.ibatis.annotations.*;

@org.apache.ibatis.annotations.Mapper
public interface DashboardMapper {
  @Select(
      "select (select count(*) from owner where status='ACTIVE') owners,(select count(*) from pet"
          + " where status='ACTIVE') pets,(select count(*) from vet where status='ACTIVE')"
          + " vets,(select count(*) from visit where status='SCHEDULED') scheduledVisits")
  Map<String, Object> summary();

  @Select(
      "select date(created_at) label,count(*) value from visit where"
          + " created_at>=date_sub(current_date,interval 6 day) group by date(created_at) order by"
          + " label")
  List<Map<String, Object>> visitTrend();

  @Select(
      "select pt.name label,count(*) value from pet p join pet_type pt on pt.id=p.type_id group by"
          + " pt.id,pt.name order by value desc")
  List<Map<String, Object>> petDistribution();

  @Select(
      "select v.name label,count(vi.id) value from vet v left join visit vi on vi.vet_id=v.id group"
          + " by v.id,v.name order by value desc limit 8")
  List<Map<String, Object>> vetWorkload();

  @Select(
      "select vi.id,p.name petName,v.name vetName,s.start_time startTime,vi.status from visit vi"
          + " join pet p on p.id=vi.pet_id join vet v on v.id=vi.vet_id join vet_schedule_slot s on"
          + " s.id=vi.slot_id order by vi.id desc limit 8")
  List<Map<String, Object>> recentVisits();
}
