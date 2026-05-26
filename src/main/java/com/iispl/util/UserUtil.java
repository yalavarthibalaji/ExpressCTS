package com.iispl.util;

import java.util.ArrayList;

import java.util.List;

import com.iispl.dto.UserModel;

/**
 * UserUtil.java Hardcoded user credential registry — matches the prototype's
 * ROLES object exactly.
 *
 * Package : com.iispl.util Pattern : Utility (pure static helper, no business
 * logic)
 *
 * To switch to DB: Replace USERS list with a JDBC call in findByCredentials().
 *
 * Credentials (from prototype): maker1 / maker123 → Maker (Data Entry) checker1
 * / check123 → Checker (Verifier) supervisor1 / super123 → CTS Supervisor
 * cts_admin / cts123 → CTS Admin microperator1 / microp123 → MICR Operator
 * iw_maker1 / iwmaker123 → Inward Maker (MICR R/R) iw_sup1 / iwsup123 → Inward
 * Supervisor tech1 / tech1123 → Technical Verifier - I tech2 / tech2123 →
 * Technical Verifier - II bm1 / bm123 → Branch Manager
 */
public class UserUtil {

	private static final List<UserModel> USERS = new ArrayList<>();

	static {
		// ── Outward Clearing Roles ──────────────────────────────────────
		USERS.add(new UserModel("maker1", "maker123", "maker", "Maker (Data Entry)", "M", "✏️", "ow-create-batch"));
		USERS.add(new UserModel("checker1", "check123", "checker", "Checker (Verifier)", "C", "✅", "ow-checker"));
		USERS.add(new UserModel("supervisor1", "super123", "supervisor", "CTS Supervisor", "V", "👁", "dashboard"));
		USERS.add(new UserModel("cts_admin", "cts123", "cts", "CTS Admin", "A", "🏦", "dashboard"));
		USERS.add(
				new UserModel("microperator1", "microp123", "micr_op", "MICR Operator", "MO", "🔧", "ow-create-batch"));

		// ── Inward Clearing Roles ───────────────────────────────────────
		USERS.add(
				new UserModel("iw_maker1", "iwmaker123", "iw_maker", "Inward Maker (MICR R/R)", "IM", "🔬", "iw-micr"));
		USERS.add(new UserModel("iw_sup1", "iwsup123", "iw_supervisor", "Inward Supervisor", "S", "📥", "iw-receive"));
		USERS.add(new UserModel("tech1", "tech1123", "tech1", "Technical Verifier - I", "T1", "🔍", "iw-officer"));
		USERS.add(new UserModel("tech2", "tech2123", "tech2", "Technical Verifier - II", "T2", "✍️", "iw-sig"));
		USERS.add(new UserModel("bm1", "bm123", "bm", "Branch Manager", "B", "🏛", "bm-dashboard"));
	}

	/**
	 * Authenticate: match userId + password exactly.
	 * 
	 * @return UserModel on success, null if credentials don't match.
	 */
	public static UserModel findByCredentials(String userId, String password) {
		if (userId == null || password == null)
			return null;
		for (UserModel user : USERS) {
			if (user.getUserId().equals(userId.trim()) && user.getPassword().equals(password.trim())) {
				return user;
			}
		}
		return null;
	}

	/**
	 * Lookup by userId only (used for role-detection preview while typing).
	 */
	public static UserModel findByUserId(String userId) {
		if (userId == null)
			return null;
		for (UserModel user : USERS) {
			if (user.getUserId().equals(userId.trim()))
				return user;
		}
		return null;
	}

	// Prevent instantiation
	private UserUtil() {
	}
}