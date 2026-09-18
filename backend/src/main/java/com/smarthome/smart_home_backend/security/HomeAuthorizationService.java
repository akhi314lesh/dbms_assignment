package com.smarthome.smart_home_backend.security;

import com.smarthome.smart_home_backend.entity.AutomationRule;
import com.smarthome.smart_home_backend.entity.HomeAccess;
import com.smarthome.smart_home_backend.entity.Room;
import com.smarthome.smart_home_backend.entity.User;
import com.smarthome.smart_home_backend.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class HomeAuthorizationService {

    private final HomeRepository homeRepository;
    private final HomeAccessRepository homeAccessRepository;
    private final RoomRepository roomRepository;
    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final AutomationRuleRepository automationRuleRepository;
    private final RuleActionRepository ruleActionRepository;

    public HomeAuthorizationService(
            HomeRepository homeRepository,
            HomeAccessRepository homeAccessRepository,
            RoomRepository roomRepository,
            DeviceRepository deviceRepository,
            AlertRepository alertRepository,
            AutomationRuleRepository automationRuleRepository,
            RuleActionRepository ruleActionRepository) {

        this.homeRepository = homeRepository;
        this.homeAccessRepository = homeAccessRepository;
        this.roomRepository = roomRepository;
        this.deviceRepository = deviceRepository;
        this.alertRepository = alertRepository;
        this.automationRuleRepository = automationRuleRepository;
        this.ruleActionRepository = ruleActionRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof FirebaseUserDetails userDetails) {
            return userDetails.getUser();
        } else if (principal instanceof User user) {
            return user;
        }
        return null;
    }

    public User requireCurrentUser() {
        User user = getCurrentUser();
        if (user == null) {
            throw new AccessDeniedException("Full authentication is required to access this resource.");
        }
        return user;
    }

    public boolean isAdmin(User user) {
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public boolean isCurrentUserAdmin() {
        return isAdmin(getCurrentUser());
    }

    public List<Long> getAccessibleHomeIds(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        if (isAdmin(user)) {
            return homeRepository.findAllHomeIds();
        }
        return homeAccessRepository.findHomeIdsByUserId(user.getUserId());
    }

    public List<Long> getAccessibleHomeIds() {
        return getCurrentUserAccessibleHomeIds();
    }

    public List<Long> getCurrentUserAccessibleHomeIds() {
        return getAccessibleHomeIds(requireCurrentUser());
    }

    public boolean canAccessHome(User user, Long homeId) {
        if (user == null || homeId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return homeAccessRepository.existsByUserUserIdAndHomeHomeId(user.getUserId(), homeId);
    }

    public boolean canManageHome(User user, Long homeId) {
        if (user == null || homeId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        Optional<HomeAccess> access = homeAccessRepository.findByUserUserIdAndHomeHomeId(user.getUserId(), homeId);
        return access.map(ha -> "OWNER".equalsIgnoreCase(ha.getRole())).orElse(false);
    }

    public boolean canAccessRoom(User user, Long roomId) {
        if (user == null || roomId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        return roomOpt.map(r -> canAccessHome(user, r.getHome().getHomeId())).orElse(false);
    }

    public boolean canManageRoom(User user, Long roomId) {
        if (user == null || roomId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        return roomOpt.map(r -> canManageHome(user, r.getHome().getHomeId())).orElse(false);
    }

    public boolean canAccessDevice(User user, Long deviceId) {
        if (user == null || deviceId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return deviceRepository.findHomeIdByDeviceId(deviceId)
                .map(homeId -> canAccessHome(user, homeId))
                .orElse(false);
    }

    public boolean canManageDevice(User user, Long deviceId) {
        if (user == null || deviceId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return deviceRepository.findHomeIdByDeviceId(deviceId)
                .map(homeId -> canManageHome(user, homeId))
                .orElse(false);
    }

    public boolean canAccessReading(User user, Long deviceId, Long readingId) {
        return canAccessDevice(user, deviceId);
    }

    public boolean canAccessAlert(User user, Long alertId) {
        if (user == null || alertId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return alertRepository.findHomeIdByAlertId(alertId)
                .map(homeId -> canAccessHome(user, homeId))
                .orElse(false);
    }

    public boolean canManageAlert(User user, Long alertId) {
        if (user == null || alertId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return alertRepository.findHomeIdByAlertId(alertId)
                .map(homeId -> canManageHome(user, homeId))
                .orElse(false);
    }

    public boolean canAccessRule(User user, Long ruleId) {
        if (user == null || ruleId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        Optional<AutomationRule> ruleOpt = automationRuleRepository.findById(ruleId);
        if (ruleOpt.isEmpty()) {
            return false;
        }
        AutomationRule rule = ruleOpt.get();
        if (rule.getCreatedByUser() != null && user.getUserId().equals(rule.getCreatedByUser().getUserId())) {
            return true;
        }
        if (rule.getConditionDevice() != null) {
            return canAccessDevice(user, rule.getConditionDevice().getDeviceId());
        }
        return false;
    }

    public boolean canAccessAction(User user, Long actionId) {
        if (user == null || actionId == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return ruleActionRepository.findHomeIdByActionId(actionId)
                .map(homeId -> canAccessHome(user, homeId))
                .orElse(false);
    }

    public boolean canAccessPreference(User user, Long targetUserId, Long deviceId) {
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return user.getUserId().equals(targetUserId) && canAccessDevice(user, deviceId);
    }

    public boolean canAccessUserProfile(User user, Long targetUserId) {
        if (user == null) {
            return false;
        }
        if (isAdmin(user)) {
            return true;
        }
        return user.getUserId().equals(targetUserId);
    }

    // --- Assertion Helpers (Throw AccessDeniedException -> HTTP 403) ---

    public void assertCanAccessHome(Long homeId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessHome(user, homeId)) {
            if (homeRepository.existsById(homeId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanManageHome(Long homeId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canManageHome(user, homeId)) {
            throw new AccessDeniedException("Access is denied. Home OWNER or ADMIN role required.");
        }
    }

    public void assertCanAccessRoom(Long roomId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessRoom(user, roomId)) {
            if (roomRepository.existsById(roomId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanManageRoom(Long roomId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canManageRoom(user, roomId)) {
            throw new AccessDeniedException("Access is denied. Home OWNER or ADMIN role required.");
        }
    }

    public void assertCanAccessDevice(Long deviceId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessDevice(user, deviceId)) {
            if (deviceRepository.existsById(deviceId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanManageDevice(Long deviceId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canManageDevice(user, deviceId)) {
            throw new AccessDeniedException("Access is denied. Home OWNER or ADMIN role required.");
        }
    }

    public void assertCanAccessReading(Long deviceId, Long readingId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessReading(user, deviceId, readingId)) {
            if (deviceRepository.existsById(deviceId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanAccessAlert(Long alertId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessAlert(user, alertId)) {
            if (alertRepository.existsById(alertId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanManageAlert(Long alertId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canManageAlert(user, alertId)) {
            throw new AccessDeniedException("Access is denied. Home OWNER or ADMIN role required.");
        }
    }

    public void assertCanAccessRule(Long ruleId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessRule(user, ruleId)) {
            if (automationRuleRepository.existsById(ruleId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanAccessAction(Long actionId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessAction(user, actionId)) {
            if (ruleActionRepository.existsById(actionId)) {
                throw new AccessDeniedException("Access is denied.");
            }
        }
    }

    public void assertCanAccessPreference(Long targetUserId, Long deviceId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessPreference(user, targetUserId, deviceId)) {
            throw new AccessDeniedException("Access is denied.");
        }
    }

    public void assertCanAccessUserProfile(Long targetUserId) {
        User user = requireCurrentUser();
        if (isAdmin(user)) return;
        if (!canAccessUserProfile(user, targetUserId)) {
            throw new AccessDeniedException("Access is denied.");
        }
    }
}
