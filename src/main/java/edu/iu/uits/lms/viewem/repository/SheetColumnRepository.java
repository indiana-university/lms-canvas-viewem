package edu.iu.uits.lms.viewem.repository;

import edu.iu.uits.lms.viewem.model.SheetColumn;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chmaurer on 6/4/15.
 */
//@RepositoryRestResource(collectionResourceRel = "sheetColumns", path = "sheetColumns")
@Component
public interface SheetColumnRepository extends PagingAndSortingRepository<SheetColumn, Long> {

    List<SheetColumn> findBySheet(String sheetId);
}
