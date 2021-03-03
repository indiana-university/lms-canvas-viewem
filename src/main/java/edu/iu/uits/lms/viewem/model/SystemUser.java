package edu.iu.uits.lms.viewem.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Created by chmaurer on 7/28/15.
 */
@Entity
@Table(name = "LMS_VIEWEM_SYSTEM_USER",
        uniqueConstraints = @UniqueConstraint(name = "UK_SYSTEMUSER_SYSTEMID_USERID", columnNames = {"userId", "systemId"}))
@SequenceGenerator(name = "LMS_VIEWEM_SYSTEM_USER_ID_SEQ", sequenceName = "LMS_VIEWEM_SYSTEM_USER_ID_SEQ", allocationSize = 1)
@NamedQueries({
        @NamedQuery(name = "SystemUser.findByUserAndSystem", query = "from SystemUser where userId = :userId and systemId = :systemId")})
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class SystemUser extends ModelWithDates {

    @Id
    @GeneratedValue(generator = "LMS_VIEWEM_SYSTEM_USER_ID_SEQ")
    @Column(name = "VIEWEM_SYSTEM_USER_ID")
    private Long systemUserId;

    @NonNull
    @Column(name = "userid")
    private String userId;

    @NonNull
    @Column(name = "userfullname")
    private String userFullName;

    @NonNull
    @Column(name = "usersortablename")
    private String userSortableName;

    @NonNull
    @Column(name = "systemid")
    private String systemId;

}
