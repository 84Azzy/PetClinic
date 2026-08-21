package com.zzy.petclinic.visit;

import com.zzy.petclinic.common.*;

public interface VisitService {
  Visit create(VisitRequest request);

  PageResponse<Visit> page(PageQuery query, Long petId, Long vetId);

  java.util.List<Visit> mine();

  Visit get(Long id);

  Visit cancel(Long id, CancelVisitRequest request);

  Visit complete(Long id);
}
