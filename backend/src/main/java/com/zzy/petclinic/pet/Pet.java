package com.zzy.petclinic.pet;

import com.baomidou.mybatisplus.annotation.TableName;
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
  private String photoUrl;
  private String status;
}
