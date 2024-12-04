package edu.iu.uits.lms.viewem.model;

/*-
 * #%L
 * lms-canvas-viewem
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chmaurer on 6/4/15.
 */

@Entity
@Table(name = "LMS_VIEWEM_SHEET")
@NamedQueries({
    @NamedQuery(name = "Sheet.findByContextAndSystem", query = "from Sheet where context = :context and systemId = :systemId and deleted = false " +
            "order by modifiedOn desc"),
    @NamedQuery(name = "Sheet.findByContextAndPublishedAndSystem", query = "from Sheet where published = :published " +
            "and context = :context and systemId = :systemId and deleted = false order by modifiedOn desc"),
    @NamedQuery(name = "Sheet.findByContextAndPublishedAndSystemAndUser", query = "select s from Sheet s JOIN s.sheetUsers su " +
            "where s.published = :published and s.context = :context and s.systemId = :systemId and su.userId = :userId and deleted = false " +
            "order by modifiedOn desc")
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
