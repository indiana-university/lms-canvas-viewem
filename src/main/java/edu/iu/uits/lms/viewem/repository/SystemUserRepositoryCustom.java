package edu.iu.uits.lms.viewem.repository;

import edu.iu.uits.lms.viewem.model.SystemUser;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Created by chmaurer on 1/25/16.
 */
public interface SystemUserRepositoryCustom {

    List<SystemUser> findByUsersAndSystem(@Param("userIds") List<String> userIds, @Param("systemId") String systemId);
}
