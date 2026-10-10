package com.zzy.petclinic.pet;

import com.zzy.petclinic.common.*;
import com.zzy.petclinic.storage.ImageResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Tag(name = "宠物档案（学习者实现）")
@RestController
@RequestMapping("/api/pets")
public class PetController {

  @Autowired
  private PetService petService;

  @Autowired
  private PetImageService petImageService;

  @Operation(summary = "新增宠物")
  @PostMapping
  @PreAuthorize("hasAuthority('pet:create') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Pet> create(@Valid @RequestBody PetRequest r) {
    return ApiResponse.created(petService.create(r));
  }

  @Operation(summary = "分页查询宠物")
  @GetMapping
  @PreAuthorize("hasAuthority('pet:manage')")
  public ApiResponse<PageResponse<Pet>> page(
      @Valid PageQuery q, @RequestParam(required = false) Long ownerId) {
    /*
     * 不恰当：Controller 根据 accountType 分支并执行资源归属判断，其他入口直接调用 Service 时可以绕过。
     * 原 page(...) 中的身份判断和 ownerId 收敛逻辑已下沉到 PetServiceImpl.page(...)。
     */
    return ApiResponse.ok(petService.page(q, ownerId));
  }

  @Operation(summary = "查询我的宠物，宠物主人专用")
  @GetMapping("/mine")
  @PreAuthorize("hasRole('OWNER')")
  public ApiResponse<List<Pet>> mine() {
    // 不恰当：把 principal 的 userId 从 Controller 传入 Service，Service 仍可能被其他调用方传入任意用户编号。
    // return ApiResponse.ok(petService.mine(user.getId()));
    return ApiResponse.ok(petService.mine());
  }

  @Operation(summary = "查询宠物详情")
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('pet:manage')")
  public ApiResponse<Pet> get(@PathVariable Long id) {
    return ApiResponse.ok(petService.get(id));
  }

  @Operation(summary = "修改宠物")
  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Pet> update(@PathVariable Long id, @Valid @RequestBody PetRequest r) {
    return ApiResponse.ok(petService.update(id,r));
  }

  @Operation(summary = "停用宠物")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('pet:delete') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    petService.delete(id);
    return ApiResponse.message("删除成功");
  }

  /**
   * 接收前端 {@code FormData} 中名为 {@code file} 的字段。
   *
   * <p>此接口只接收浏览器上传，不让浏览器直接访问 COS；文件内容校验和对象 Key 生成均在后端完成。
   */
  @Operation(summary = "上传或替换宠物照片")
  @PostMapping(path = "/{id}/photo", consumes = "multipart/form-data")
  @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Pet> uploadPhoto(
      @PathVariable Long id, @RequestPart("file") MultipartFile file) {
    return ApiResponse.ok(petImageService.replace(id, file));
  }

  /**
   * 经 JWT 和业务权限校验后，把私有 COS 图片代理给浏览器。
   *
   * <p>响应体是图片二进制，不是 {@link ApiResponse} JSON。
   */
  @Operation(summary = "读取宠物照片")
  @GetMapping("/{id}/photo")
  @PreAuthorize("hasAuthority('pet:manage')")
  public ResponseEntity<StreamingResponseBody> photo(@PathVariable Long id) {
    return ImageResponses.stream(petImageService.load(id));
  }

  /** 清空数据库对象 Key，并清理对应 COS 对象。 */
  @Operation(summary = "删除宠物照片")
  @DeleteMapping("/{id}/photo")
  @PreAuthorize("hasAuthority('pet:update') && hasAnyRole('ADMIN', 'STAFF')")
  public ApiResponse<Void> deletePhoto(@PathVariable Long id) {
    petImageService.delete(id);
    return ApiResponse.message("照片已删除");
  }

}
