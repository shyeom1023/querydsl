package jpabook.jpashop.repository;


import static jpabook.jpashop.entity.QMember.member;
import static jpabook.jpashop.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;


import jpabook.jpashop.dto.MemberSerachCondition;
import jpabook.jpashop.dto.MemberTeamDto;
import jpabook.jpashop.dto.QMemberTeamDto;
import jpabook.jpashop.entity.Member;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class MemberJpaRepository {
	
	private final EntityManager em;
	private final JPAQueryFactory queryFactory;
	
	//@RequiredArgsConstructor 이걸로 아래꺼 처리함
//	public MemberJpaRepository(EntityManager em,JPAQueryFactory queryFactory) {
//		super();
//		this.em = em;
//		this.queryFactory = queryFactory;
//	}
	
	public void save(Member member) {
		em.persist(member);
	}
	
	public Optional<Member> findById(Long id){
		Member findMember = em.find(Member.class, id);
		return Optional.ofNullable(findMember);
	}
	
	public List<Member> findAll(){
		return em.createQuery("select m from Member m",Member.class).getResultList();
	}
	
	public List<Member> findByUsername(String username){
		return em.createQuery("select m from Member m where m.username = :username" , Member.class)
				.setParameter("username", username)
				.getResultList();
	}
	
	public List<Member> findAll_Querydsl(){
		return queryFactory
					.select(member)
					.from(member)
					.fetch();
					
	}
	
	public List<Member> findByUsername_Querydsl(String username){
		return queryFactory
				.select(member)
				.from(member)
				.where(member.username.eq(username))
				.fetch();
	}
	
	public List<MemberTeamDto> serachByBuilder(MemberSerachCondition condition){
		
		BooleanBuilder builder = new BooleanBuilder();
		if(hasText(condition.getUsername())) {
			builder.and(member.username.eq(condition.getUsername()));
		}
		
		if(hasText(condition.getTaemName())) {
			builder.and(team.name.eq(condition.getTaemName()));
		}
		if(condition.getAgeGoe() != null) {
			builder.and(member.age.goe(condition.getAgeGoe()));
		}
		
		if(condition.getAgeLoe() != null) {
			builder.and(member.age.loe(condition.getAgeLoe()));
		}
		
		return queryFactory
					.select(new QMemberTeamDto(
							member.id.as("memberId"),
							member.username,
							member.age,
							team.id.as("teamId"),
							team.name.as("teamName")
							))
					.from(member)
					.leftJoin(member.team, team)
					.where(builder)
					.fetch();
					
	}
	
	

}
