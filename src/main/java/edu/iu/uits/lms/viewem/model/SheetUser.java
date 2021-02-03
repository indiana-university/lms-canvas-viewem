package edu.iu.uits.lms.viewem.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chmaurer on 6/12/15.
 */
@Entity
@Table(name = "LMS_VIEWEM_SHEET_USER")
@SequenceGenerator(name = "LMS_VIEWEM_SHEET_USER_ID_SEQ", sequenceName = "LMS_VIEWEM_SHEET_USER_ID_SEQ", allocationSize = 1)
@NamedQueries({
        @NamedQuery(name = "SheetUser.findBySheet", query = "from SheetUser where sheet.sheetId = :sheetId order by userId asc"),
        @NamedQuery(name = "SheetUser.findByUserAndSheet", query = "from SheetUser where userId = :userId and sheet.sheetId = :sheetId"),
        @NamedQuery(name = "SheetUser.getUserIdsBySheet", query = "select su.userId from SheetUser su where su.sheet.sheetId = :sheetId")})
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@EqualsAndHashCode(exclude = {"sheet"})
@ToString(exclude = {"sheet"})
public class SheetUser {

    @Id
    @GeneratedValue(generator = "LMS_VIEWEM_SHEET_USER_ID_SEQ")
    @Column(name = "VIEWEM_SHEET_USER_ID")
    private Long sheetUserId;

    @NonNull
    @Column(name = "userid")
    private String userId;

    @OneToMany(cascade = CascadeType.ALL, targetEntity = UserData.class, mappedBy = "sheetUser", fetch = FetchType.EAGER, orphanRemoval = true)
    @OrderColumn(name = "sequence")
    private List<UserData> data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VIEWEM_SHEET_ID")
    @RestResource(exported = false)
    private Sheet sheet;

    /**
     * Convenience method to add a UserData to the current SheetUser
     * @param userData UserData to add
     */
    public void addUserData(UserData userData) {
        if (data == null) {
            data = new ArrayList<UserData>();
        }
        userData.setSheetUser(this);
        data.add(userData);
    }
}
