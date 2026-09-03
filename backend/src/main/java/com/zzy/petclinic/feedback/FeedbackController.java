package com.zzy.petclinic.feedback;

import com.zzy.petclinic.common.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "意见反馈")
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
  private final FeedbackService s;

  @GetMapping
  @PreAuthorize("hasAuthority('feedback:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<PageResponse<Feedback>> page(@Valid PageQuery q) {
    return ApiResponse.ok(s.page(q));
  }

  @GetMapping("/mine")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PageResponse<Feedback>> mine(@Valid PageQuery q) {
    return ApiResponse.ok(s.mine(q));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('feedback:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Feedback> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<Feedback> create(@Valid @RequestBody FeedbackRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PostMapping("/{id}/reply")
  @PreAuthorize("hasAuthority('feedback:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Feedback> reply(
      @PathVariable Long id, @Valid @RequestBody ReplyFeedbackRequest r) {
    return ApiResponse.ok(s.reply(id, r));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('feedback:manage') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Feedback> close(@PathVariable Long id) {
    return ApiResponse.ok(s.close(id));
  }
}
