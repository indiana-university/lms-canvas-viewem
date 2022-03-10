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
