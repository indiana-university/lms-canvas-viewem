package edu.iu.uits.lms.viewem.service;

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

import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.viewem.model.SystemUser;
import edu.iu.uits.lms.viewem.repository.SystemUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by chmaurer on 7/28/15.
 */
@Service
@Slf4j
public class SystemUserService {

    @Autowired
    private SystemUserRepository systemUserRepository = null;

    /**
     * Method to keep a list of user's names up-to-date
     * @param users List of Users to use
     * @param systemId System id where the users are associated
     */
    public void createOrUpdateUsers(List<User> users, String systemId) {
        List<String> userIds = new ArrayList<>();
        Map<String, User> userMap = new HashMap<>();
        Map<String, SystemUser> systemUserMap = new HashMap<>();

        for (User user : users) {
        	if (user.getLoginId() != null) {
        		userIds.add(user.getLoginId());
        		userMap.put(user.getLoginId(), user);
        	}
        }

        //Get all existing users
        List<SystemUser> systemUsers = systemUserRepository.findByUsersAndSystem(userIds, systemId);
        for (SystemUser systemUser : systemUsers) {
            systemUserMap.put(systemUser.getUserId(), systemUser);
        }

        List<SystemUser> usersToSave = new ArrayList<>();

        //Find users that we need to create or update
        for (Map.Entry<String, User> entry : userMap.entrySet()) {
            String userId = entry.getKey();
            User user = entry.getValue();
            SystemUser existingSystemUser = systemUserMap.get(userId);
            String name = user.getName();
            String sortableName = user.getSortableName();

            if (existingSystemUser == null) {
                existingSystemUser = new SystemUser(userId, name, sortableName, systemId);
                usersToSave.add(existingSystemUser);
                log.debug("Creating new user entry - " + existingSystemUser);
            } else if (!existingSystemUser.getUserFullName().equals(name) || !existingSystemUser.getUserSortableName().equals(sortableName)) {
                existingSystemUser.setUserFullName(name);
                existingSystemUser.setUserSortableName(sortableName);
                usersToSave.add(existingSystemUser);
                log.debug("Updating user entry - " + existingSystemUser);
            } else {
                log.debug("No user changes needed for " + existingSystemUser);
            }
        }

        //bulk save
        systemUserRepository.saveAll(usersToSave);
    }
}
