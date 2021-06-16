package jpabook.jpashop;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jpabook.jpashop.entity.Member;
import jpabook.jpashop.entity.QMember;
import jpabook.jpashop.entity.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

	@Autowired
	EntityManager em;
	
	JPAQueryFactory jpaQueryFactory;
	
	@BeforeEach
	public void before() {
		jpaQueryFactory =new JPAQueryFactory(em);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);
		
		Member member1 = new Member("member1",10,teamA);
		Member member2 = new Member("member2",20,teamA);
		Member member3 = new Member("member3",30,teamB);
		Member member4 = new Member("member4",40,teamB);
		
		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);
		
	}
	
	@Test
	public void StartJPQL() {
		//member1을 찾아라.
		Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
			.setParameter("username", "member1")
			.getSingleResult();
		
		assertThat(findByJPQL.getUsername()).isEqualTo("member1");
	}
	
	@Test
	public void startQuerydsl() {
		
		QMember qMember = new QMember("m");
		
		Member findMember = jpaQueryFactory
			.select(qMember)
			.from(qMember)
			.where(qMember.username.eq("member1"))//파라미터 바인딩 처리
			.fetchOne();
		
		assertThat(findMember.getUsername()).isEqualTo("member1");
	}
	
	
	
	
	
	
	
	
	
	
}
