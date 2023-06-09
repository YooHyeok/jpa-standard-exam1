package jpql.statement;

import jpql.domain.Member;
import jpql.domain.MemberType;
import jpql.domain.Team;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.List;

public class JpqlLv2 {

    /**
     * 벌크연산 : 다량의 update 혹은 delete 처리<br/>
     * executeUpdateQuery()<br/>
     * 주의 : 영속성 컨텍스트를 무시하고 데이터베이스에 직접 쿼리를 날린다.<br/>
     * update 혹은 delete는 flush를 먼저 호출하기 때문에 벌크 연산을 먼저 실행한다.<br/>
     * 만약 캐시에 데이터가 존재하는 경우는 벌크연산 수행 후 영속성 컨텍스트를 초기화 해야한다.(초기화 후 다시 조회-캐시에적재)<br/>
     */
    public void bulkOperation(EntityManager em) {
        Member member1 = new Member();
        member1.setUsername("회원1");
        em.persist(member1);
        Member member2 = new Member();
        member2.setUsername("회원2");
        em.persist(member2);
        Member member3 = new Member();
        member3.setUsername("회원3");
        em.persist(member3);
        em.flush();
        em.clear();

        int updateCount1 = em.createQuery("update Member m set m.age = 20")
                .executeUpdate();
        System.out.println("updateCount1 = " + updateCount1);
        em.clear();

        int updateCount2 = em.createQuery("update Member m set m.age = 30 where m.age < :age")
                .setParameter("age",21)
                .executeUpdate();
        System.out.println("updateCount2 = " + updateCount2);
        em.clear();

        int deleteCount = em.createQuery("delete Member m where m.age=30").executeUpdate();
        System.out.println("deleteCount = " + deleteCount);

        List<Member> selectMFromMemberM = em.createQuery("select m from Member m", Member.class).getResultList();
        System.out.println("selectMFromMemberM = " + selectMFromMemberM);
    }

    /**
     * NamedQuery 엔티티 클래스에 @NamedQuery어노테이션을 선언한다<br/>
     * @NamedQuery(name = '' , query = '')<br/>
     * persistence.xml에 <mapping-file>META-INF/ormMember.xml</mapping-file> 선언 <br/>
     * <provider></provider> 태그 다음위치에 선언해야한다.<br/>
     * XML이 항상 우선권을 가진다.<br/>
     */
    public void namedQuery(EntityManager em) {
        Member member = new Member();
        member.setUsername("memberA");
        member.setAge(32);
        member.setType(MemberType.ADMIN);
        em.persist(member);
        em.flush();
        em.clear();
        Member findByUsername = em.createNamedQuery("Member.findByUsername", Member.class)
                .setParameter("username", "memberA")
                .getSingleResult();
        System.out.println("findByUsername = " + findByUsername);
        em.clear();
        Member findByAge = em.createNamedQuery("Member.findByAge", Member.class)
                .setParameter("age", 32)
                .getSingleResult();
        System.out.println("findByAge = " + findByAge);
        em.clear();

        Long singleResult = em.createNamedQuery("Member.count", Long.class)
                .getSingleResult();
        System.out.println("singleResult = " + singleResult);
        em.clear();
    }

    /**
     * fetch inner join
     * fetch left join
     * Collection Fetch Join (Distinct)
     * @param em
     */
    public void fetchJoin(EntityManager em) {
        //패치조인
        // innerJoin
        Team team1 = new Team();
        team1.setName("TeamA");
        em.persist(team1);
        Team team2 = new Team();
        team2.setName("TeamB");
        em.persist(team2);
        Member member1 = new Member();
        member1.setUsername("회원1");
        member1.setTeam(team1);
        em.persist(member1);
        Member member2 = new Member();
        member2.setUsername("회원2");
        member2.setTeam(team1);
        em.persist(member2);
        Member member3 = new Member();
        member3.setUsername("회원3");
        member3.setTeam(team2);
        em.persist(member3);
/*        Member member4 = new Member();
        member4.setUsername("회원4");
        member4.setTeam(null);
        em.persist(member4);*/
        em.flush();
        em.clear();

        // 일반적인 조회 지연로딩에서 member.getTeam().getName()은 한번 조회한 뒤에는 Proxy로 발생
        List<Member> lazyLoadingResult = em.createQuery("select m from Member m ", Member.class).getResultList();
        System.out.println("[lazyLoadingResult]");
        for (Member member : lazyLoadingResult) {
            System.out.println("member = " + member.getUsername() + ", " + ((member.getTeam().getName() == null) ? "null" : member.getTeam().getName()));
        }
        em.clear();

        // 단순 innerJoin : 지연로딩 이므로 join을 사용하여도 n+1현상 발생 (Select절에 지정한 엔티티만 조회할 뿐 연관관계에 있는 엔티티를 함께 조회하지 않는다.)
        List<Member> innerJoinResult = em.createQuery("select m from Member m join m.team t", Member.class).getResultList();
        System.out.println("[innerJoinResult]");
        for (Member member : innerJoinResult) {
            System.out.println("member = " + member.getUsername() + ", " + member.getTeam().getName());
        }
        em.clear();

        // fetch inner join : 이때의 member.getTeam().getName()은 Proxy가 아니다. (연관관계에 있는 엔티티를 함께 조회한다.)
        List<Member> innerFetchJoinResult = em.createQuery("select m from Member m join fetch m.team t", Member.class).getResultList();
        System.out.println("[innerFetchJoinResult]");
        for (Member member : innerFetchJoinResult) {
            System.out.println("member = " + member.getUsername() + ", " + member.getTeam().getName());
        }
        em.clear();

        //fetch left join
        List<Member> leftFetchJoinResult = em.createQuery("select m from Member m left join fetch m.team t", Member.class).getResultList();
        System.out.println("[leftFetchJoinResult]");
        for (Member member : leftFetchJoinResult) {
            System.out.println("member = " + member.getUsername() + ", " + member.getTeam().getName());
        }
        em.clear();

        // Collection Fetch Join : 일대다관계, 컬렉션
        // 일반적인 쿼리에서는 id를 기준으로 한번씩만 조회되지만 join대상인 memberList는 컬렉션이기 때문에 두개씩 조회된다.
        List<Team> collectionFetchJoinResult = em.createQuery("select t from Team t join fetch t.memberList", Team.class).getResultList();
        System.out.println("[collectionFetchJoinResult]");
        for (Team team : collectionFetchJoinResult) {
            System.out.println("team = " + team.getName() + " | members = " + team.getMemberList().size());
            for (Member member : team.getMemberList()) {
                System.out.println("\t member = " + member.getUsername());
            }
        }
        em.clear();

        // 패치조인과 Distinct : JPQL - SQL의 Distinct와 애플리케이션에서 엔티티 중복 제거하는 2가지 기능을 제거
        List<Team> distinctCollectionFetchJoinResult = em.createQuery("select distinct t from Team t join fetch t.memberList", Team.class).getResultList();
        System.out.println("[distinctCollectionFetchJoinResult]");
        for (Team team : distinctCollectionFetchJoinResult) {
            System.out.println("team = " + team.getName() + " | members = " + team.getMemberList().size());
            for (Member member : team.getMemberList()) {
                System.out.println("\t member = " + member.getUsername());
            }
        }
        em.clear();
    }

    /** 엔티티 별칭에 .(점)을 찍어 객체 그래프를 탐색하는 것 <br/> (상태필드, 단일값 연관경로, 컬렉션 값 연관 경로)*/
    public void pathExpression(EntityManager em) {
        //상태 필드 : 경로 탐색의 끝으로 더이상 탐색이 되지 않는다.
        Member member = new Member();
        member.setUsername("아아아");
        em.persist(member);
        em.flush();
        em.clear();
        List<String> staticFieldResult = em.createQuery("select m.username from Member m", String.class).getResultList();
        System.out.println("staticFieldResult = " + staticFieldResult);
        em.clear();

        //단일 값 연관 경로 : 묵시적 내부조인이 발생한다.(위험하므로 추천하지 않는 방법)
        Team team = new Team();
        team.setName("바보");
        em.persist(team);
        Member member1 = new Member();
        member1.setUsername("아아아");
        member1.setTeam(team);
        em.persist(member1);
        Member member2 = new Member();
        member2.setUsername("아아아");
        member2.setTeam(team);
        em.persist(member2);
        em.flush();
        em.clear();
        List<String> singleValueResult = em.createQuery("select m.team.name from Member m", String.class).getResultList();
        System.out.println("singleValueResult = " + singleValueResult);

        //컬렉션 값 연관 경로 : 묵시적 내부조인이 발생한다. 더이상 탐색할 수 없다.(위험하므로 명시적으로 조인하도록 한다.)
        /** 묵시적 조인은 조인이 일어나는 상황을 한눈에 파악하기 어렵다 */
        List<Collection> collectionValueResult = em.createQuery("select t.memberList from Team t", Collection.class).getResultList();
        System.out.println("collectionValueResult = " + collectionValueResult);

        //컬렉션 값 연관 경로 : 명시적 조인 - Team의 member와 Team을 join한 뒤 추출
        List<Collection> stat = em.createQuery("select m.username from Team t join t.member m", Collection.class).getResultList();
    }
}
