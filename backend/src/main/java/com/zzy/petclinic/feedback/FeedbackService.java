package com.zzy.petclinic.feedback;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzy.petclinic.auth.CurrentUser;
import com.zzy.petclinic.common.*;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FeedbackService {
  private final FeedbackMapper mapper;
  private final CurrentUser user;

  public PageResponse<Feedback> page(PageQuery q) {
    LambdaQueryWrapper<Feedback> w =
        new LambdaQueryWrapper<Feedback>().orderByDesc(Feedback::getId);
    if (StringUtils.hasText(q.keyword()))
      w.and(
          x ->
              x.like(Feedback::getTitle, q.keyword()).or().like(Feedback::getContent, q.keyword()));
    if (StringUtils.hasText(q.status())) w.eq(Feedback::getStatus, q.status());
    return PageResponse.of(mapper.selectPage(Page.of(q.pageValue(), q.sizeValue()), w));
  }

  public PageResponse<Feedback> mine(PageQuery q) {
    return PageResponse.of(
        mapper.selectPage(
            Page.of(q.pageValue(), q.sizeValue()),
            new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, user.id())
                .orderByDesc(Feedback::getId)));
  }

  public Feedback get(Long id) {
    Feedback x = mapper.selectById(id);
    if (x == null) throw BusinessException.notFound("反馈");
    return x;
  }

  @Transactional
  public Feedback create(FeedbackRequest r) {
    Feedback x = new Feedback();
    x.setUserId(user.id());
    x.setCategory(r.category());
    x.setTitle(r.title());
    x.setContent(r.content());
    x.setContact(r.contact());
    x.setStatus("PENDING");
    x.setCreatedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.insert(x);
    return x;
  }

  @Transactional
  public Feedback reply(Long id, ReplyFeedbackRequest r) {
    Feedback x = get(id);
    x.setReply(r.reply());
    x.setStatus("REPLIED");
    x.setRepliedBy(user.id());
    x.setRepliedAt(LocalDateTime.now());
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }

  @Transactional
  public Feedback close(Long id) {
    Feedback x = get(id);
    x.setStatus("CLOSED");
    x.setUpdatedAt(LocalDateTime.now());
    mapper.updateById(x);
    return x;
  }
}
