package edu.iu.uits.lms.viewem.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Created by chmaurer on 6/11/15.
 */
@Entity
@Table(name = "LMS_VIEWEM_SHEET_COLUMN")
@SequenceGenerator(name = "LMS_VIEWEM_SHEET_COLUMN_ID_SEQ", sequenceName = "LMS_VIEWEM_SHEET_COLUMN_ID_SEQ", allocationSize = 1)
@NamedQuery(name = "SheetColumn.findBySheet", query = "from SheetColumn where sheetId = ?1")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"sheet"})
@ToString(exclude = {"sheet"})
public class SheetColumn {
    /**
     * database field size for the title
     */
    public static int SHEET_COLUMN_TITLE_SIZE = 255;

    @Id
    @GeneratedValue(generator = "LMS_VIEWEM_SHEET_COLUMN_ID_SEQ")
    @Column(name = "VIEWEM_SHEET_COLUMN_ID")
    private Long sheetColumnId;

    @NonNull
    private String title;

    private int sequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VIEWEM_SHEET_ID")
    @RestResource(exported = false)
    private Sheet sheet;
}
