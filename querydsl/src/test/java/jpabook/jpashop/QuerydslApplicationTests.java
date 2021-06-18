package jpabook.jpashop;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jpabook.jpashop.entity.Hello;
import jpabook.jpashop.entity.QHello;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

	@Autowired
	EntityManager em;
	
	
	@Test
	void contextLoads() {
		
		Hello hello = new Hello();
		em.persist(hello);
		
		JPAQueryFactory query = new JPAQueryFactory(em);
		QHello qHello = new QHello("h");
		
		Hello result = query
			.selectFrom(qHello)
			.fetchOne();
		
		assertThat(result).isEqualTo(hello);
		assertThat(result.getId()).isEqualTo(hello.getId());
	}

}
