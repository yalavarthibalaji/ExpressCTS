/*Role-based navigation This program done by Ramana Login page is linked*/
package com.iispl.dto;

public class UserModel {

	// ── Login credentials (matched against UserUtil.ROLES) ──────────
	private String userId; // e.g. "maker1"
	private String password; // e.g. "maker123"

	// ── Role info ────────────────────────────────────────────────────
	private String roleId; // e.g. "maker"
	private String roleLabel; // e.g. "Maker (Data Entry)"
	private String roleInitial; // e.g. "M" — shown in header avatar
	private String roleIcon; // e.g. "✏️" — shown in detected-role panel

	// ── Default landing section after login ──────────────────────────
	private String defaultSection; // e.g. "ow-create-batch"

	// ── Session info ─────────────────────────────────────────────────
	private String lastLoginTime; // stored in session, shown on next login
	private boolean active; // true = ACTIVE user, false = DISABLED

	// ================================================================
	// Constructors
	// ================================================================

	public UserModel() {
	}

	/**
	 * Full constructor used by AuthService when credentials match.
	 */
	public UserModel(String userId, String password, String roleId, String roleLabel, String roleInitial,
			String roleIcon, String defaultSection) {
		this.userId = userId;
		this.password = password;
		this.roleId = roleId;
		this.roleLabel = roleLabel;
		this.roleInitial = roleInitial;
		this.roleIcon = roleIcon;
		this.defaultSection = defaultSection;
		this.active = true;
	}

	// ================================================================
	// Getters & Setters
	// ================================================================

	public String getUserId() {
		return userId;
	}

	public void setUserId(String v) {
		this.userId = v;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String v) {
		this.password = v;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String v) {
		this.roleId = v;
	}

	public String getRoleLabel() {
		return roleLabel;
	}

	public void setRoleLabel(String v) {
		this.roleLabel = v;
	}

	public String getRoleInitial() {
		return roleInitial;
	}

	public void setRoleInitial(String v) {
		this.roleInitial = v;
	}

	public String getRoleIcon() {
		return roleIcon;
	}

	public void setRoleIcon(String v) {
		this.roleIcon = v;
	}

	public String getDefaultSection() {
		return defaultSection;
	}

	public void setDefaultSection(String v) {
		this.defaultSection = v;
	}

	public String getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(String v) {
		this.lastLoginTime = v;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean v) {
		this.active = v;
	}

	@Override
	public String toString() {
		return "UserModel{userId='" + userId + "', roleId='" + roleId + "', roleLabel='" + roleLabel + "'}";
	}
}
