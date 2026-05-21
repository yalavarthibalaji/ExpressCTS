package com.iispl.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

import com.iispl.model.UserModel;

/**
 * HeaderController.java ZK MVC Composer — controls components/header.zul
 *
 * Package : com.iispl.controller Pattern : ZK MVC (SelectorComposer) Role :
 * Reusable header component controller
 *
 * Reads the "loggedInUser" session attribute (UserModel) and populates the
 * header avatar, user name, and role badge. Also handles the Sign Out (logout)
 * button.
 */
public class HeaderController extends SelectorComposer<Component> {

	// ── Wired components from header.zul ──────────────────────────────
	@Wire("#headerRoleBadge")
	private Div headerRoleBadge;
	@Wire("#headerAvatar")
	private Div headerAvatar;
	@Wire("#headerUserName")
	private Label headerUserName;
	@Wire("#btnHeaderLogout")
	private org.zkoss.zul.Button btnHeaderLogout;

	// ── Lifecycle ─────────────────────────────────────────────────────

	@Override
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);

		// Read the logged-in user from the ZK session
		UserModel user = (UserModel) Sessions.getCurrent().getAttribute("loggedInUser");

		if (user == null) {
			// No session — redirect to login
			Executions.sendRedirect("/zul/login.zul");
			return;
		}

		// Populate header fields from the user session object
		headerAvatar.setStyle("background:#1a3a6e;color:#fff;border-radius:50%;"
				+ "width:34px;height:34px;display:flex;align-items:center;"
				+ "justify-content:center;font-weight:700;font-size:14px;");
		headerAvatar.appendChild(new org.zkoss.zul.Label(user.getRoleInitial()));
		headerUserName.setValue(user.getUserId());
		headerRoleBadge.appendChild(new org.zkoss.zul.Label(user.getRoleIcon() + " " + user.getRoleLabel()));
	}

	// ── Logout button ─────────────────────────────────────────────────

	@Listen("onClick = #btnHeaderLogout")
	public void onLogout(Event e) {
		// Invalidate the ZK session — clears all session attributes
		Sessions.getCurrent().invalidate();
		// Redirect to login page
		Executions.sendRedirect("/zul/login.zul");
	}
}