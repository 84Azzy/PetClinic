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

  @JsonProperty("photoUrl")
  public String getPhotoUrl() {
    return photoObjectKey == null || getId() == null ? null : "/api/pets/" + getId() + "/photo";
  }
}
