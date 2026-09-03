package com.zzy.petclinic.notice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.rbac.authentication.CurrentUser;
import com.zzy.petclinic.common.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoticeService {
  private final NoticeMapper mapper;
  private final CurrentUser user;

  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public PageResponse<Notice> page(PageQuery q) {
    LambdaQueryWrapper<Notice> w =
        new LambdaQueryWrapper<Notice>()
            .orderByDesc(Notice::getSortOrder)
            .orderByDesc(Notice::getId);
    if (StringUtils.hasText(q.keyword())) w.like(Notice::getTitle, q.keyword());
    if (StringUtils.hasText(q.status())) w.eq(Notice::getStatus, q.status());
    return PageResponse.of(mapper.selectPage(Page.of(q.pageValue(), q.sizeValue()), w));
  }

  @PreAuthorize("isAuthenticated()")
  public List<Notice> active() {
    LocalDateTime now = LocalDateTime.now();
    return mapper.selectList(
        new LambdaQueryWrapper<Notice>()
            .eq(Notice::getStatus, "PUBLISHED")
            .and(x -> x.isNull(Notice::getExpiresAt).or().gt(Notice::getExpiresAt, now))
            .orderByDesc(Notice::getSortOrder)
            .orderByDesc(Notice::getPublishedAt));
  }

  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public Notice get(Long id) {
    Notice x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound("公告");
    return x;
  }

  @Transactional
  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public Notice create(NoticeRequest r) {
    Notice x = new Notice();
    apply(x, r);
    x.setStatus("DRAFT");
    x.setCreatedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.insert(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public Notice update(Long id, NoticeRequest r) {
    Notice x = get(id);
    apply(x, r);
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public Notice publish(Long id) {
    Notice x = get(id);
    x.setStatus("PUBLISHED");
    x.setPublishedAt(LocalDateTime.now());
    x.setPublisherId(user.id());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public Notice withdraw(Long id) {
    Notice x = get(id);
    x.setStatus("WITHDRAWN");
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  @PreAuthorize("hasAuthority('notice:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public void delete(Long id) {
    mapper.deleteById(get(id));
  }

  private void apply(Notice x, NoticeRequest r) {
    x.setTitle(r.title());
    x.setContent(r.content());
    x.setExpiresAt(r.expiresAt());
    x.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
  }
}
