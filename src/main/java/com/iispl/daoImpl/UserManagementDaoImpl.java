package com.iispl.daoImpl;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.iispl.dao.UserManagementDao;
import com.iispl.db.HibernateUtil;
import com.iispl.entity.SystemRole;
import com.iispl.entity.SystemUser;
import com.iispl.entity.UserRequest;
import com.iispl.entity.UserRole;

public class UserManagementDaoImpl implements UserManagementDao {

    @Override
    public void saveUser(SystemUser user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(user);
            transaction.commit();
            System.out.println("User saved: " + user.getUserId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save user: " + e.getMessage());
        }
    }

    @Override
    public void updateUser(SystemUser user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
            System.out.println("User updated: " + user.getUserId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to update user: " + e.getMessage());
        }
    }

    @Override
    public SystemUser findByUserId(String userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SystemUser> query = session.createQuery(
                "FROM SystemUser WHERE userId = :userId", SystemUser.class);
            query.setParameter("userId", userId);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find user: " + e.getMessage());
        }
    }

    @Override
    public SystemUser findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(SystemUser.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find user by id: " + e.getMessage());
        }
    }

    @Override
    public List<SystemUser> getAllUsers() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SystemUser> query = session.createQuery(
                "FROM SystemUser ORDER BY createdAt DESC", SystemUser.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get all users: " + e.getMessage());
        }
    }

    @Override
    public List<SystemUser> getUsersByBranch(String branchCode) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SystemUser> query = session.createQuery(
                "FROM SystemUser WHERE branchCode = :branchCode ORDER BY fullName",
                SystemUser.class);
            query.setParameter("branchCode", branchCode);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get users by branch: " + e.getMessage());
        }
    }

    @Override
    public void saveUserRole(UserRole userRole) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(userRole);
            transaction.commit();
            System.out.println("UserRole saved for user: "
                + userRole.getSystemUser().getUserId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save user role: " + e.getMessage());
        }
    }

    @Override
    public List<SystemRole> getAllRoles() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SystemRole> query = session.createQuery(
                "FROM SystemRole WHERE active = true ORDER BY roleLabel",
                SystemRole.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get roles: " + e.getMessage());
        }
    }

    @Override
    public SystemRole findRoleByCode(String roleCode) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<SystemRole> query = session.createQuery(
                "FROM SystemRole WHERE roleCode = :roleCode", SystemRole.class);
            query.setParameter("roleCode", roleCode);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find role: " + e.getMessage());
        }
    }

    @Override
    public SystemRole findRoleById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(SystemRole.class, id);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find role by id: " + e.getMessage());
        }
    }

    @Override
    public UserRole findUserRole(Long systemUserId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<UserRole> query = session.createQuery(
                "FROM UserRole WHERE systemUser.id = :userId", UserRole.class);
            query.setParameter("userId", systemUserId);
            return query.setMaxResults(1).uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find user role: " + e.getMessage());
        }
    }

    @Override
    public void updateUserRole(Long systemUserId, Long newRoleId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Query query = session.createQuery(
                "UPDATE UserRole SET systemRole.id = :roleId " +
                "WHERE systemUser.id = :userId");
            query.setParameter("roleId", newRoleId);
            query.setParameter("userId", systemUserId);
            query.executeUpdate();

            transaction.commit();
            System.out.println("UserRole updated for userId: " + systemUserId);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to update user role: " + e.getMessage());
        }
    }

    @Override
    public boolean userIdExists(String userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(u) FROM SystemUser u WHERE u.userId = :userId",
                Long.class);
            query.setParameter("userId", userId);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to check userId: " + e.getMessage());
        }
    }
    
    
    //Giri added
    
    @Override
    public void deleteUser(String userId) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
 
            // Step 1: delete the UserRole mapping first
            // (must delete child before parent to avoid FK constraint error)
            Query<?> deleteRoleQuery = session.createQuery(
                "DELETE FROM UserRole ur WHERE ur.systemUser.userId = :userId");
            deleteRoleQuery.setParameter("userId", userId);
            deleteRoleQuery.executeUpdate();
 
            // Step 2: delete the user itself
            Query<?> deleteUserQuery = session.createQuery(
                "DELETE FROM SystemUser u WHERE u.userId = :userId");
            deleteUserQuery.setParameter("userId", userId);
            deleteUserQuery.executeUpdate();
 
            transaction.commit();
            System.out.println("User deleted: " + userId);
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }
 
    @Override
    public void saveRequest(UserRequest request) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(request);
            transaction.commit();
            System.out.println("Request saved: " + request.getRequestId());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to save request: " + e.getMessage());
        }
    }
 
    @Override
    public void updateRequest(UserRequest request) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(request);
            transaction.commit();
            System.out.println("Request updated: " + request.getRequestId()
                + " status: " + request.getStatus());
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            e.printStackTrace();
            throw new RuntimeException("Failed to update request: " + e.getMessage());
        }
    }
 
    @Override
    public List<UserRequest> getPendingRequests() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<UserRequest> query = session.createQuery(
                "FROM UserRequest WHERE status = 'PENDING' ORDER BY requestDate DESC",
                UserRequest.class);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get pending requests: " + e.getMessage());
        }
    }
 
    @Override
    public UserRequest findRequestById(String requestId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<UserRequest> query = session.createQuery(
                "FROM UserRequest WHERE requestId = :requestId", UserRequest.class);
            query.setParameter("requestId", requestId);
            return query.uniqueResult();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to find request: " + e.getMessage());
        }
    }
}