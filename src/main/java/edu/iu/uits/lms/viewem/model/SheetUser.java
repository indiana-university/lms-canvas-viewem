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

import edu.iu.uits.lms.viewem.service.RestResource;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
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
public class SheetUser implements Serializable {

    @Id
    @GeneratedValue(generator = "LMS_VIEWEM_SHEET_USER_ID_SEQ")
    @Column(name = "VIEWEM_SHEET_USER_ID")
    private Long sheetUserId;

    @NonNull
    @Column(name = "userid")
    private String userId;

    @ElementCollection(fetch = FetchType.EAGER)
    //TODO Wait to use the legit table until we're ready to deploy to REG
    @CollectionTable(name = "LMS_VIEWEM_USER_DATA_2", joinColumns = @JoinColumn(name = "VIEWEM_SYSTEM_USER_ID", foreignKey = @ForeignKey(name = "FK_VIEWEM_SYSTEM_USER_ID")))
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
        data.add(userData);
    }
}
