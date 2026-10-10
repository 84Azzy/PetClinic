package com.zzy.petclinic.vet;

import com.zzy.petclinic.common.*;
import com.zzy.petclinic.storage.ImageResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "兽医管理")
@RestController
@RequestMapping("/api/vets")
@RequiredArgsConstructor
public class VetController {
  private final VetService s;
  private final VetImageService imageService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<PageResponse<Vet>> page(
      @Valid PageQuery q, @RequestParam(required = false) Long specialtyId) {
    return ApiResponse.ok(s.page(q, specialtyId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<Vet> get(@PathVariable Long id) {
    return ApiResponse.ok(s.get(id));
  }

  @GetMapping("/{id}/specialties")
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<List<Long>> specialties(@PathVariable Long id) {
    return ApiResponse.ok(s.specialties(id));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('vet:manage')")
  public ApiResponse<Vet> create(@Valid @RequestBody VetRequest r) {
    return ApiResponse.created(s.create(r));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('vet:manage')")
  public ApiResponse<Vet> update(@PathVariable Long id, @Valid @RequestBody VetRequest r) {
    return ApiResponse.ok(s.update(id, r));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('vet:manage')")
  public ApiResponse<Void> disable(@PathVariable Long id) {
    s.disable(id);
    return ApiResponse.message("兽医已停用");
  }

  /** 接收 {@code FormData.file} 并由后端完成校验、COS 上传和数据库 Key 更新。 */
  @PostMapping(path = "/{id}/avatar", consumes = "multipart/form-data")
  @PreAuthorize("hasAuthority('vet:manage')")
  public ApiResponse<Vet> uploadAvatar(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(imageService.replace(id, file));
  }

  /** 经 JWT 鉴权后，将私有 COS 头像以二进制流返回给浏览器。 */
  @GetMapping("/{id}/avatar")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<StreamingResponseBody> avatar(@PathVariable Long id) {
    return ImageResponses.stream(imageService.load(id));
  }

  /** 清空数据库对象 Key，并清理对应 COS 对象。 */
  @DeleteMapping("/{id}/avatar")
  @PreAuthorize("hasAuthority('vet:manage')")
  public ApiResponse<Void> deleteAvatar(@PathVariable Long id) {
    imageService.delete(id);
    return ApiResponse.message("头像已删除");
  }
}
