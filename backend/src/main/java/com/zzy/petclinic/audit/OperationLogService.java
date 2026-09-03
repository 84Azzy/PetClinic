package com.zzy.petclinic.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('system:manage')")
public class OperationLogService {
  private final OperationLogMapper mapper;

  public PageResponse<OperationLog> page(PageQuery query) {
    return PageResponse.of(
        mapper.selectPage(
            Page.of(query.pageValue(), query.sizeValue()),
            new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getId)));
  }
}
