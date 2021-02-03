package edu.iu.uits.lms.viewem.repository;

import edu.iu.uits.lms.viewem.model.Sheet;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chmaurer on 6/4/15.
 */
//@RepositoryRestResource(collectionResourceRel = "sheets", path = "sheets")
@Component
public interface SheetRepository extends PagingAndSortingRepository<Sheet, Long> {

    /**
     * Get the list of (un)published Sheet objects within the system and context
     * @param context Context that the sheet is in
     * @param published Flag indicating if the sheets you want are published or not
     * @param systemId SystemId that the sheet is in
     * @return The list of (un)published Sheet objects within the system and context
     */
    List<Sheet> findByContextAndPublishedAndSystem(@Param("context") String context, @Param("published") boolean published,
                                                   @Param("systemId") String systemId);

    /**
     * Get the list of (un)published Sheet objects that the user has access to within the system and context
     * @param context Context that the sheet is in
     * @param published Flag indicating if the sheets you want are published or not
     * @param systemId SystemId that the sheet is in
     * @param userId UserId will need to be in the Data (SheetUser) of the sheet
     * @return The list of (un)published Sheet objects that the user has access to within the system and context
     */
    List<Sheet> findByContextAndPublishedAndSystemAndUser(@Param("context") String context, @Param("published") boolean published,
                                                   @Param("systemId") String systemId, @Param("userId") String userId);

    /**
     * Get the list of sheets within the system and context
     * @param context Context that the sheet is in
     * @param systemId SystemId that the sheet is in
     * @return The list of Sheet objects within the system and context
     */
    List<Sheet> findByContextAndSystem(@Param("context") String context, @Param("systemId") String systemId);
}
