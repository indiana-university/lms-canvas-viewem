package edu.iu.uits.lms.viewem.service;

import canvas.client.generated.model.User;
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
