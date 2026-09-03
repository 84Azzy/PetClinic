package com.zzy.petclinic.owner;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.common.BusinessException;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OwnerService {
  private final OwnerMapper mapper;
  private final CurrentUser currentUser;

  public PageResponse<Owner> page(PageQuery query) {
    LambdaQueryWrapper<Owner> w = new LambdaQueryWrapper<Owner>().orderByDesc(Owner::getId);
    if (StringUtils.hasText(query.keyword()))
      w.and(
          x -> x.like(Owner::getName, query.keyword()).or().like(Owner::getPhone, query.keyword()));
    if (StringUtils.hasText(query.status())) w.eq(Owner::getStatus, query.status());
    return PageResponse.of(mapper.selectPage(Page.of(query.pageValue(), query.sizeValue()), w));
  }

  public Owner get(Long id) {
    Owner value = mapper.selectById(id);
    if (value == null) throw BusinessException.notFound("主人档案");
    return value;
  }

  public Owner mine() {
    Owner value =
        mapper.selectOne(new LambdaQueryWrapper<Owner>().eq(Owner::getUserId, currentUser.id()));
    if (value == null) throw BusinessException.notFound("主人档案");
    return value;
  }

  @Transactional
  public Owner create(OwnerRequest r) {
    Owner value = new Owner();
    apply(value, r);
    value.setStatus(r.status() == null ? "ACTIVE" : r.status());
    value.setCreatedAt(LocalDateTime.now());
    value.setUpdatedAt(LocalDateTime.now());
    mapper.insert(value);
    return value;
  }

  @Transactional
  public Owner update(Long id, OwnerRequest r) {
    Owner value = get(id);
    apply(value, r);
    value.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(value);
    return value;
  }

  @Transactional
  public Owner updateMine(OwnerRequest r) {
    Owner value = mine();
    apply(
        value,
        new OwnerRequest(
            value.getUserId(), r.name(), r.phone(), r.email(), r.address(), value.getStatus()));
    value.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(value);
    return value;
  }

  @Transactional
  public void disable(Long id) {
    Owner value = get(id);
    value.setStatus("INACTIVE");
    value.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(value);
  }

  private void apply(Owner v, OwnerRequest r) {
    v.setUserId(r.userId());
    v.setName(r.name());
    v.setPhone(r.phone());
    v.setEmail(r.email());
    v.setAddress(r.address());
    if (r.status() != null) v.setStatus(r.status());
  }
}
