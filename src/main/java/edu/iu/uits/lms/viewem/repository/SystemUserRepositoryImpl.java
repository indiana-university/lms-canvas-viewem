package edu.iu.uits.lms.viewem.repository;

import edu.iu.uits.lms.viewem.model.SystemUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by chmaurer on 1/25/16.
 */
@Slf4j
public class SystemUserRepositoryImpl implements SystemUserRepositoryCustom {
    @Autowired
//    @Qualifier(value = "viewemEntityManagerFactory")
    private EntityManager entityManager = null;

    private static final int ITEM_LIMIT = 999;


    @Override
    public List<SystemUser> findByUsersAndSystem(@Param("userIds") List<String> userIds, @Param("systemId") String systemId) {
        log.debug("findByUsersAndSystem");

//        EntityManager entityManager = null;
        try {
//            entityManager = viewemEntityManagerFactory.createEntityManager();

            //@NamedQuery(name = "SystemUser.findByUsersAndSystem", query = "from SystemUser where userId in :userIds and systemId = :systemId order by userSortableName")})

            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<SystemUser> q = criteriaBuilder.createQuery(SystemUser.class);

            //From
            Root<SystemUser> from = q.from(SystemUser.class);

            //Select
            CriteriaQuery<SystemUser> select = q.select(from);

            //Where systemId
            ParameterExpression<String> p_system = criteriaBuilder.parameter(String.class, "systemId");
            Predicate systemIdPredicate = criteriaBuilder.equal(from.get("systemId"), p_system);

            //Where userId list
            Predicate finalUserIdsPredicate = null;
            Map<ParameterExpression<List>, List<String>> userIdParamMap = new HashMap<>();
            if (userIds.size() > ITEM_LIMIT) {
                List<List<String>> sublists = ListUtils.partition(userIds, ITEM_LIMIT);
                int index = 0;
                List<Predicate> orPredicates = new ArrayList<>();
                for (List<String> sublist : sublists) {

                    ParameterExpression<List> p_userIds = criteriaBuilder.parameter(List.class, "userIds" + index);
                    orPredicates.add(criteriaBuilder.or(from.get("userId").in(p_userIds)));
                    userIdParamMap.put(p_userIds, sublist);
                    index++;
                }
                finalUserIdsPredicate = criteriaBuilder.or(orPredicates.toArray(new Predicate[orPredicates.size()]));

            } else {
                ParameterExpression<List> p_userIds = criteriaBuilder.parameter(List.class, "userIds");
                finalUserIdsPredicate = from.get("userId").in(p_userIds);
                userIdParamMap.put(p_userIds, userIds);
            }

            //Build the full where clause
            q.select(from).where(criteriaBuilder.and(systemIdPredicate,
                    finalUserIdsPredicate));

            //Order by
            select.orderBy(criteriaBuilder.asc(from.get("userSortableName")));

            //Create the query and set params
            TypedQuery<SystemUser> query = entityManager.createQuery(select);
            query.setParameter(p_system, systemId);
            for (Map.Entry<ParameterExpression<List>, List<String>> entry : userIdParamMap.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            String resultSql = query.unwrap(org.hibernate.Query.class).getQueryString();
            log.debug(resultSql);

            List<SystemUser> users = query.getResultList();

            return users;
        } finally {
            if (entityManager != null) {
                entityManager.close();
            }
        }

    }
}
