package com.zzy.petclinic.vet;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zzy.petclinic.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vet")
public class Vet extends BaseEntity {
  private String name;
  private String phone;
  private String email;
  private String licenseNo;
  private String biography;
  @JsonIgnore private String avatarObjectKey;
  private String status;

  @JsonProperty("avatarUrl")
  public String getAvatarUrl() {
    return avatarObjectKey == null || getId() == null ? null : "/api/vets/" + getId() + "/avatar";
  }
}
