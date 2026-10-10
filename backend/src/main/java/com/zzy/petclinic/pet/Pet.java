package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zzy.petclinic.common.BaseEntity;
import java.time.LocalDate;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pet")
public class Pet extends BaseEntity {
  private Long ownerId;
  private Long typeId;
  private String name;
  private String gender;
  private String breed;
  private LocalDate birthDate;
  private String color;
  private String microchipNo;
  private String allergies;
  @JsonIgnore private String photoObjectKey;
  private String status;

  /**
   * 生成前端读取照片所需的后端 API 地址。
   *
   * <p>{@link JsonIgnore} 保证真实 COS 对象 Key 不出现在接口 JSON 中；本方法通过 {@link JsonProperty} 暴露稳定的
   * {@code photoUrl}。前端因此不需要知道桶名、地域和 Key，也不会接触 COS 凭证。
   *
   * @return 有照片时返回 {@code /api/pets/{id}/photo}，否则返回 {@code null}
   */
  @JsonProperty("photoUrl")
  public String getPhotoUrl() {
    return photoObjectKey == null || getId() == null ? null : "/api/pets/" + getId() + "/photo";
  }
}
