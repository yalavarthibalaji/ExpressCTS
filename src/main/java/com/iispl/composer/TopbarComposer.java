package com.iispl.composer;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

public class TopbarComposer extends SelectorComposer<Component> {

	@Wire("#userName")
	private Label userName;

	@Wire("#userRole")
	private Label userRole;

	@Wire("#userAvatar")
	private Div userAvatar;

	@Wire("#logoutBtn")
	private Button logoutBtn;

	@Override
	public void doAfterCompose(Component comp) throws Exception {
	    super.doAfterCompose(comp);

	    Session session = Sessions.getCurrent();
	    String name = (String) session.getAttribute("userName");
	    String role = (String) session.getAttribute("userRole");

	    if (name != null && !name.isBlank()) {
	        userName.setValue(name);
	        String initials = name.length() >= 2
	                ? name.substring(0, 2).toUpperCase()
	                : name.toUpperCase();
	        userAvatar.appendChild(new Label(initials));
	    }

	    if (role != null) {
	        userRole.setValue(role);
	    }
	}
	@Listen("onClick=#logoutBtn")
    public void doLogout() {
        Sessions.getCurrent().invalidate();
        Executions.sendRedirect("/login/login.zul");
    }
}