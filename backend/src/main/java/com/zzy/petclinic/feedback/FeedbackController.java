package com.zzy.petclinic.feedback;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "意见反馈")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
  private final FeedbackService s;

  @GetMapping
  public ApiResponse<PageResponse<Feedback>> page(@Valid PageQuery q) {
    return ApiResponse.ok(s.page(q));
  }

  @GetMapping("/mine")
  public ApiResponse<PageResponse<Feedback>> mine(@Valid PageQuery q) {
    return ApiResponse.ok(s.mine(q));
  }

  @GetMapping("/{id}")
  public ApiResponse<Feedback> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @PostMapping
  public ApiResponse<Feedback> create(@Valid @RequestBody FeedbackRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PostMapping("/{id}/reply")
  public ApiResponse<Feedback> reply(
      @PathVariable Long id, @Valid @RequestBody ReplyFeedbackRequest r) {
    return ApiResponse.ok(s.reply(id, r));
  }

  @PostMapping("/{id}/close")
  public ApiResponse<Feedback> close(@PathVariable Long id) {
    return ApiResponse.ok(s.close(id));
  }
}
