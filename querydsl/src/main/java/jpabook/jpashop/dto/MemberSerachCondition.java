package jpabook.jpashop.dto;

import lombok.Data;

@Data
public class MemberSerachCondition {
	//회원명, 팀명, 나이(ageGoe, ageLoe)
	private String username;
	private String taemName;
	private Integer ageGoe;
	private Integer ageLoe;

}
