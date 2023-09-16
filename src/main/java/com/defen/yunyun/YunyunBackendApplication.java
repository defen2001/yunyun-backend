package com.defen.yunyun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.defen.yunyun.mapper")
public class YunyunBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(YunyunBackendApplication.class, args);
	}

}
