package edu.iu.uits.lms.viewem.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chmaurer on 6/4/15.
 */

@Entity
@Table(name = "LMS_VIEWEM_SHEET")
@NamedQueries({
    @NamedQuery(name = "Sheet.findByContextAndSystem", query = "from Sheet where context = :context and systemId = :systemId and deleted = 0 " +
            "order by modifiedon desc"),
    @NamedQuery(name = "Sheet.findByContextAndPublishedAndSystem", query = "from Sheet where published = :published " +
            "and context = :context and systemId = :systemId and deleted = 0 order by modifiedon desc"),
    @NamedQuery(name = "Sheet.findByContextAndPublishedAndSystemAndUser", query = "select s from Sheet s JOIN s.sheetUsers su " +
            "where s.published = :published and s.context = :context and s.systemId = :systemId and su.userId = :userId and deleted = 0" +
            "order by modifiedon desc")
})
@SequenceGenerator(name = "LMS_VIEWEM_SHEET_ID_SEQ", sequenceName = "LMS_VIEWEM_SHEET_ID_SEQ", allocationSize = 1)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class Sheet extends ModelWithDates {

    @Id
    @GeneratedValue(generator = "LMS_VIEWEM_SHEET_ID_SEQ")
    @Column(name = "VIEWEM_SHEET_ID")
    private Long sheetId;

    @NonNull
    private String title;

    @NonNull
    private String context;

    @NonNull
    @Column(name = "systemid")
    private String systemId;

    @NonNull
    private boolean published;

    @NonNull
    @Column(name = "createdby")
    private String createdBy;

    @NonNull
    @Column(name = "modifiedby")
    private String modifiedBy;

    private boolean deleted;


    @OneToMany(cascade = CascadeType.ALL, targetEntity = SheetColumn.class, mappedBy = "sheet", fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderColumn(name = "sequence")
    private List<SheetColumn> columns;

    @OneToMany(cascade = CascadeType.ALL, targetEntity = SheetUser.class, mappedBy = "sheet", orphanRemoval = true)
    private List<SheetUser> sheetUsers;


    /**
     * Convenience method to add a SheetColumn to the current Sheet
     * @param sheetColumn SheetColumn to add
     */
    public void addColumn(SheetColumn sheetColumn) {
        if (columns == null) {
            columns = new ArrayList<SheetColumn>();
        }
        sheetColumn.setSheet(this);
        columns.add(sheetColumn);
    }

    /**
     * Convenience method to add a SheetUser to the current Sheet
     * @param sheetUser SheetUser to add
     */
    public void addSheetUser(SheetUser sheetUser) {
        if (sheetUsers == null) {
            sheetUsers = new ArrayList<SheetUser>();
        }
        sheetUser.setSheet(this);
        sheetUsers.add(sheetUser);
    }
}
