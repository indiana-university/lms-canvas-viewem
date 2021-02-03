package edu.iu.uits.lms.viewem.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

/**
 * Created by chmaurer on 7/28/15.
 */
@Data
@MappedSuperclass
public class ModelWithDates {

    @Column(name = "createdon")
    private Date createdOn;
    @Column(name = "modifiedon")
    private Date modifiedOn;


    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
        modifiedOn = new Date();
        if (createdOn==null) {
            createdOn = new Date();
        }
    }
}
