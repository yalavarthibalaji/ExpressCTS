package com.iispl.serviceImpl;

import com.iispl.dao.UserSessionDao;
import com.iispl.daoImpl.UserSessionDaoImpl;
import com.iispl.entity.User;
import com.iispl.entity.UserSession;
import com.iispl.service.SessionService;

import java.util.List;
import java.util.UUID;

public class SessionServiceImpl implements SessionService {

    private final UserSessionDao sessionDao = new UserSessionDaoImpl();

    @Override
    public String startSession(Long userId, String ipAddress) {
        if (userId == null) return null;

        UserSession s = new UserSession();
        User userRef = new User();
        userRef.setId(userId);
        s.setUser(userRef);

        // Random session token. Stored in DB + HTTP session for matching.
        String token = UUID.randomUUID().toString();
        s.setSessionToken(token);
        s.setIpAddress(ipAddress != null ? ipAddress : "unknown");

        // login_at is set by @PrePersist on the entity (LocalDateTime.now())

        UserSession saved = sessionDao.save(s);
        if (saved == null) {
            System.err.println("SessionService → startSession FAILED for userId="
                    + userId);
            return null;
        }
        System.out.println("SessionService → startSession: userId=" + userId
                + " token=" + token + " ip=" + ipAddress);
        return token;
    }

    @Override
    public boolean endSession(String sessionToken) {
        return sessionDao.expireByToken(sessionToken);
    }

    @Override
    public int endAllSessionsForUser(Long userId) {
        return sessionDao.expireAllForUser(userId);
    }

    @Override
    public List<UserSession> getActiveSessions(Long userId) {
        return sessionDao.findActiveByUser(userId);
    }
}