package jpabook.jpashop;

import static com.querydsl.jpa.JPAExpressions.select;
import static jpabook.jpashop.entity.QMember.member;
import static jpabook.jpashop.entity.QTeam.team;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jpabook.jpashop.dto.MemberDto;
import jpabook.jpashop.dto.UserDto;
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
		jpaQueryFactory = new JPAQueryFactory(em);
		Team teamA = new Team("teamA");
		Team teamB = new Team("teamB");
		em.persist(teamA);
		em.persist(teamB);

		Member member1 = new Member("member1", 10, teamA);
		Member member2 = new Member("member2", 20, teamA);
		Member member3 = new Member("member3", 30, teamB);
		Member member4 = new Member("member4", 40, teamB);

		em.persist(member1);
		em.persist(member2);
		em.persist(member3);
		em.persist(member4);

	}

	@Test
	public void StartJPQL() {
		// member1을 찾아라.
		Member findByJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
				.setParameter("username", "member1").getSingleResult();

		assertThat(findByJPQL.getUsername()).isEqualTo("member1");
	}

	@Test
	public void startQuerydsl() {

//		QMember qMember = new QMember("m");

		Member findMember = jpaQueryFactory.select(member).from(member).where(member.username.eq("member1"))// 파라미터 바인딩
																											// 처리
				.fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void search() {
		Member findMember = jpaQueryFactory.selectFrom(member)
				.where(member.username.eq("member1").and(member.age.eq(10))).fetchOne();

		assertThat(findMember.getUsername()).isEqualTo("member1");
	}

	@Test
	public void resultFetch() {
		List<Member> fecth = jpaQueryFactory.selectFrom(member).fetch();

		Member fetchOne = jpaQueryFactory.selectFrom(member).where(member.age.eq(10)).fetchOne();

		Member fetchFirst = jpaQueryFactory.selectFrom(member).fetchFirst();

		QueryResults<Member> fetchResults = jpaQueryFactory.selectFrom(member).fetchResults();

		long total = fetchResults.getTotal();

		List<Member> results = fetchResults.getResults();

	}

	/**
	 * 회원 정렬 순서 1. 회원 나이 내림차순(desc) 2, 회원 이름 올림차순(asc) 단 2에서 회원 이름이 없으면 마지막에
	 * 출력(nulls last)
	 */
	@Test
	public void sort() {

		em.persist(new Member(null, 100));
		em.persist(new Member("member5", 100));
		em.persist(new Member("member6", 100));

		List<Member> result = jpaQueryFactory.selectFrom(member).where(member.age.eq(100))
				.orderBy(member.age.desc(), member.username.asc().nullsLast()).fetch();

		Member member5 = result.get(0);
		Member member6 = result.get(1);
		Member memberNull = result.get(2);
		System.out.println(member5);

		assertThat(member5.getUsername()).isEqualTo("member5");
		assertThat(member6.getUsername()).isEqualTo("member6");
		assertThat(memberNull.getUsername()).isNull();

	}

	@Test
	public void paging1() {
		List<Member> result = jpaQueryFactory.selectFrom(member).orderBy(member.username.desc()).offset(1).limit(2)
				.fetch();

		System.out.println(result.size());
		assertThat(result.size()).isEqualTo(2);

	}

	@Test
	public void paging2() {
		QueryResults<Member> queryResults = jpaQueryFactory.selectFrom(member).orderBy(member.username.desc()).offset(1)
				.limit(2).fetchResults();

		assertThat(queryResults.getTotal()).isEqualTo(4);
		assertThat(queryResults.getLimit()).isEqualTo(2);
		assertThat(queryResults.getOffset()).isEqualTo(1);
		assertThat(queryResults.getResults().size()).isEqualTo(2);

	}

	@Test
	public void aggregation() {
		List<Tuple> result = jpaQueryFactory
				.select(member.count(), member.age.sum(), member.age.avg(), member.age.max(), member.age.min()

				).from(member).fetch();

		Tuple tuple = result.get(0);
		assertThat(tuple.get(member.count())).isEqualTo(4);
		assertThat(tuple.get(member.age.sum())).isEqualTo(100);
		assertThat(tuple.get(member.age.avg())).isEqualTo(25);
		assertThat(tuple.get(member.age.max())).isEqualTo(40);
		assertThat(tuple.get(member.age.min())).isEqualTo(10);
	}

	/**
	 * 팀의 이름과 각 팀의 평균 연령을 구해라.
	 */
	@Test
	void group() throws Exception {

		List<Tuple> result = jpaQueryFactory.select(team.name, member.age.avg()).from(member).join(member.team, team)
				.groupBy(team.name).fetch();

		Tuple teamA = result.get(0);
		Tuple teamB = result.get(1);

		assertThat(teamA.get(team.name)).isEqualTo("teamA");
		assertThat(teamA.get(member.age.avg())).isEqualTo(15); // (10 + 20) / 2 = 15

		assertThat(teamB.get(team.name)).isEqualTo("teamB");
		assertThat(teamB.get(member.age.avg())).isEqualTo(35); // (30 + 40) /2 = 35

	}

	/**
	 * 팀 A에 소속된 모든 회원
	 */
	@Test
	void join() throws Exception {

		List<Member> result = jpaQueryFactory.select(member).from(member).join(member.team, team)
				.where(team.name.eq("teamA")).fetch();

		assertThat(result).extracting("username").containsExactly("member1", "member2");

	}

	/**
	 * 세타 조인 회원의 이름이 팀 이름과 같은 회원을 조회
	 */
	@Test
	void theta_join() throws Exception {

		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));

		List<Member> result = jpaQueryFactory.select(member).from(member, team).where(member.username.eq(team.name))
				.fetch();

		assertThat(result).extracting("username").containsExactly("teamA", "teamB");

	}

	/**
	 * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회 JPQL : select m, t from Member
	 * m left join m.team t on t.name = 'teamA'
	 */
	@Test
	void join_on_filtering() throws Exception {

		List<Tuple> result = jpaQueryFactory.select(member, team).from(member).leftJoin(member.team, team)
				.on(team.name.eq("teamA")).fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple =" + tuple);
		}

	}

	/**
	 * 연관관계 없는 엔티티 외부 조인 회원의 이름이 팀 이름과 같은 회원 조
	 */
	@Test
	void join_on_no_relation() throws Exception {

		em.persist(new Member("teamA"));
		em.persist(new Member("teamB"));

		List<Tuple> result = jpaQueryFactory.select(member, team).from(member).leftJoin(team)
				.on(member.username.eq(team.name)).fetch();

		for (Tuple tuple : result) {
			System.out.println("tuple =" + tuple);
		}

	}

	@PersistenceUnit
	EntityManagerFactory emf;

	@Test
	void fetchJoinNo() throws Exception {

		em.flush();
		em.clear();

		Member findMember = jpaQueryFactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("패치 조인 미적용").isFalse();

	}

	@Test
	void fetchJoinUse() throws Exception {

		em.flush();
		em.clear();

		Member findMember = jpaQueryFactory.select(member).from(member).join(member.team, team).fetchJoin()
				.where(member.username.eq("member1")).fetchOne();

		boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
		assertThat(loaded).as("패치 조인 미적용").isTrue();

	}

	/**
	 * 나이가 가장 많은 회원 조회
	 */
	@Test
	void subQuery() throws Exception {

		QMember memberSub = new QMember("memberSub");

		List<Member> result = jpaQueryFactory.select(member).from(member)
				.where(member.age.eq(JPAExpressions.select(memberSub.age.max()).from(memberSub))).fetch();

		assertThat(result).extracting("age").containsExactly(40);
	}

	/**
	 * 나이가 평균 이사인 회원
	 */
	@Test
	void subQueryGoe() throws Exception {

		QMember memberSub = new QMember("memberSub");

		List<Member> result = jpaQueryFactory.select(member).from(member)
				.where(member.age.goe(JPAExpressions.select(memberSub.age.avg()).from(memberSub))).fetch();

		assertThat(result).extracting("age").containsExactly(30, 40);
	}

	/**
	 * 나이가 평균 이사인 회원
	 */
	@Test
	void subQueryIn() throws Exception {

		QMember memberSub = new QMember("memberSub");

		List<Member> result = jpaQueryFactory.select(member).from(member)
				.where(member.age.in(JPAExpressions.select(memberSub.age).from(memberSub).where(memberSub.age.gt(10))))
				.fetch();

		assertThat(result).extracting("age").containsExactly(20, 30, 40);
	}
	
	
	@Test
	void selectSubquery() {
		
		QMember memberSub = new QMember("memberSub");
		
		List<Tuple> result = jpaQueryFactory
			.select(member.username,
						select(memberSub.age.avg()) //스태틱 임포트함
						.from(memberSub)
					)
			.from(member)
			.fetch();
		
		for(Tuple tuple : result) {
			System.out.println("tuple = "+tuple);
		}
	}
	
	@Test
	void basicCase() {
		List<String> result = jpaQueryFactory
			.select(member.age
					.when(10).then("열살")
					.when(20).then("스무살")
					.otherwise("기타")
					)
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s = " +s);
		}
	}
	
	@Test
	void complexCase() {
		List<String> result = jpaQueryFactory
			.select(new CaseBuilder()
						.when(member.age.between(0, 20)).then("0~20살")
						.when(member.age.between(21, 30)).then("21~30살")
						.otherwise("기타")
					)
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s = "+s);
		}
	}
	
	@Test
	void constant() {
		List<Tuple> result = jpaQueryFactory
			.select(member.username, Expressions.constant("A"))
			.from(member)
			.fetch();
		
		for(Tuple tuple : result) {
			System.out.println("typle = "+tuple);
		}
		
	}
	
	@Test
	void concat() {
		//{username}_{age}
		List<String> result = jpaQueryFactory
			.select(member.username.concat("_").concat(member.age.stringValue()))
			.from(member)
//			.where(member.username.eq("member1"))
			.fetch();
		
		for(String s : result) {
			System.out.println("s = "+s);
		}
	}
	
	@Test
	void simpleProjection() {
		List<String> result = jpaQueryFactory
			.select(member.username)
			.from(member)
			.fetch();
		
		for(String s : result) {
			System.out.println("s = "+s);
		}
	}
	
	@Test
	void tupleProjection() {
		List<Tuple> result = jpaQueryFactory
			.select(member.username, member.age)
			.from(member)
			.fetch();
		
		for(Tuple tuple : result) {
			String username = tuple.get(member.username);
			Integer age = tuple.get(member.age);
			System.out.println("username = "+username);
			System.out.println("age = "+age);
		}
	}
	
	@Test
	void findDtoJPQL() {
		
		List<MemberDto> result = em.createQuery("select new jpabook.jpashop.dto.MemberDto(m.username,m.age) from Member m",MemberDto.class)
			.getResultList();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = "+memberDto);
		}
	}	
	
	@Test
	void findDtoBySetter() {
		
		List<MemberDto> result = jpaQueryFactory
			.select(Projections.bean(MemberDto.class, //setter를 통해서 값이 수정됨
						member.username,
						member.age
					))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = "+memberDto);
		}
	}	
	
	@Test
	void findDtoByField() {
		
		List<MemberDto> result = jpaQueryFactory
			.select(Projections.fields(MemberDto.class, //getter setter 상관없이 값이 수정됨
						member.username,
						member.age
					))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = "+memberDto);
		}
	}	
	
	@Test
	void findUserDto() {
		
		QMember memberSub = new QMember("memberSub");
		
		List<UserDto> result = jpaQueryFactory
			.select(Projections.fields(UserDto.class, //getter setter 상관없이 값이 수정됨
						member.username.as("name"),
						ExpressionUtils.as(JPAExpressions	//age를 max로 고정되게 바꿈
								.select(memberSub.age.max())
								.from(memberSub)
								,"age"	//UserDto에 있는 age 변수명값을 넣어서 하면 됨 
								)
								
					))
			.from(member)
			.fetch();
		
		for(UserDto memberDto : result) {
			System.out.println("memberDto = "+memberDto);
		}
	}	
	
	@Test
	void findDtoByConstructor() {
		
		List<MemberDto> result = jpaQueryFactory
			.select(Projections.constructor(MemberDto.class, //getter setter 상관없이 값이 수정됨
						member.username,
						member.age
					))
			.from(member)
			.fetch();
		
		for(MemberDto memberDto : result) {
			System.out.println("memberDto = "+memberDto);
		}
	}	
}
