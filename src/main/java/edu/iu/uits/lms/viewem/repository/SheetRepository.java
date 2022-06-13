package edu.iu.uits.lms.viewem.repository;

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

import edu.iu.uits.lms.viewem.model.Sheet;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chmaurer on 6/4/15.
 */
@Component
public interface SheetRepository extends PagingAndSortingRepository<Sheet, Long> {

    /**
     * Get the list of (un)published Sheet objects within the system and context
     * @param context Context that the sheet is in
     * @param published Flag indicating if the sheets you want are published or not
     * @param systemId SystemId that the sheet is in
     * @return The list of (un)published Sheet objects within the system and context
     */
    List<Sheet> findByContextAndPublishedAndSystem(@Param("context") String context, @Param("published") boolean published,
                                                   @Param("systemId") String systemId);

    /**
     * Get the list of (un)published Sheet objects that the user has access to within the system and context
     * @param context Context that the sheet is in
     * @param published Flag indicating if the sheets you want are published or not
     * @param systemId SystemId that the sheet is in
     * @param userId UserId will need to be in the Data (SheetUser) of the sheet
     * @return The list of (un)published Sheet objects that the user has access to within the system and context
     */
    List<Sheet> findByContextAndPublishedAndSystemAndUser(@Param("context") String context, @Param("published") boolean published,
                                                   @Param("systemId") String systemId, @Param("userId") String userId);

    /**
     * Get the list of sheets within the system and context
     * @param context Context that the sheet is in
     * @param systemId SystemId that the sheet is in
     * @return The list of Sheet objects within the system and context
     */
    List<Sheet> findByContextAndSystem(@Param("context") String context, @Param("systemId") String systemId);
}
