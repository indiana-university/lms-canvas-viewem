package edu.iu.uits.lms.viewem.repository;

import edu.iu.uits.lms.viewem.model.SheetUser;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chmaurer on 6/4/15.
 */
//@RepositoryRestResource(collectionResourceRel = "sheetUsers", path = "sheetUsers")
@Component
public interface SheetUserRepository extends PagingAndSortingRepository<SheetUser, Long> {

    List<SheetUser> findBySheet(@Param("sheetId") Long sheetId);
    SheetUser findByUserAndSheet(@Param("userId") String userId, @Param("sheetId") Long sheetId);

    List<String> getUserIdsBySheet(@Param("sheetId") Long sheetId);
}
