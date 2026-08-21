package com.zzy.petclinic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(
    basePackages = "com.zzy.petclinic",
    annotationClass = org.apache.ibatis.annotations.Mapper.class,
    markerInterface = com.baomidou.mybatisplus.core.mapper.BaseMapper.class)
@SpringBootApplication(
    exclude =
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
            .class)
public class PetClinicApplication {
  public static void main(String[] args) {
    SpringApplication.run(PetClinicApplication.class, args);
  }
}
