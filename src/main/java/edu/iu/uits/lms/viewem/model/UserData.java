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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Created by chmaurer on 6/12/15.
 */
@Entity
@Table(name = "LMS_VIEWEM_USER_DATA")
@SequenceGenerator(name = "LMS_VIEWEM_USER_DATA_ID_SEQ", sequenceName = "LMS_VIEWEM_USER_DATA_ID_SEQ", allocationSize = 1)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"sheetUser"})
@ToString(exclude = {"sheetUser"})
public class UserData {
    /**
     * database field size for the data field
     */
    public static int USER_DATA_DATA_SIZE = 4000;

    @Id
    @GeneratedValue(generator = "LMS_VIEWEM_USER_DATA_ID_SEQ")
    @Column(name = "VIEWEM_USER_DATA_ID")
    private Long userGradeId;

    @NonNull
    @Column(name = "DATA", length = 4000)
    private String data;

    private int sequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VIEWEM_SHEET_USER_ID")
    @RestResource(exported = false)
    private SheetUser sheetUser;

}
