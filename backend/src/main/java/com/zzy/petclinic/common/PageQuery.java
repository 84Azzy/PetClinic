package com.zzy.petclinic.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(
    @Min(1) Long page, @Min(1) @Max(100) Long size, String keyword, String status) {
  public long pageValue() {
    return page == null ? 1 : page;
  }

  public long sizeValue() {
    return size == null ? 10 : size;
  }
}
