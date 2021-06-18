package jpabook.jpashop.dto;

import com.querydsl.core.annotations.QueryProjection;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class MemberDto {

	private String username;
	private int age;
	
	@QueryProjection
	public MemberDto(String username, int age) {
		super();
		this.username = username;
		this.age = age;
	}
	
	
	
	
}
