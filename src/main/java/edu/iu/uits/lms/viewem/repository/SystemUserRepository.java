package edu.iu.uits.lms.viewem.repository;

import edu.iu.uits.lms.viewem.model.SystemUser;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

/**
 * Created by chmaurer on 7/28/15.
 */
//@RepositoryRestResource(collectionResourceRel = "systemUsers", path = "systemUsers")
@Component
public interface SystemUserRepository extends PagingAndSortingRepository<SystemUser, Long>, SystemUserRepositoryCustom {

    SystemUser findByUserAndSystem(@Param("userId") String userId, @Param("systemId") String systemId);

}
