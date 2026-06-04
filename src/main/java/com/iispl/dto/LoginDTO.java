package com.iispl.dto;

/**
 * File    : com/iispl/dto/LoginDTO.java
 * Purpose : Carries logged-in user data in the HTTP session.
 *           Populated by UserServiceImpl after successful credential check.
 *           Used by all composers to read the current user's role and identity.
 */
public class LoginDTO {

    private Long   userId;
    private String userLoginId;
    private String fullName;
    private String roleCode;      // ADMIN | MAKER_OUTWARD | CHECKER_OUTWARD | MAKER_INWARD | CHECKER_INWARD
    private String email;

    public LoginDTO() {}

    public LoginDTO(Long userId, String userLoginId, String fullName, String roleCode, String email) {
        this.userId      = userId;
        this.userLoginId = userLoginId;
        this.fullName    = fullName;
        this.roleCode    = roleCode;
        this.email       = email;
    }

    
    public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getUserLoginId() {
		return userLoginId;
	}

	public void setUserLoginId(String userLoginId) {
		this.userLoginId = userLoginId;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(String roleCode) {
		this.roleCode = roleCode;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	/** Convenience: returns display initials from full name */
    public String getInitials() {
        if (fullName == null || fullName.isEmpty()) return "--";
        String[] parts = fullName.trim().split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
            if (sb.length() == 2) break;
        }
        return sb.toString();
    }
}
