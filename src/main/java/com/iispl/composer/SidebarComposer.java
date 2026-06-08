// File: java/com/iispl/composer/SidebarComposer.java

package com.iispl.composer;

import com.iispl.dto.LoginDTO;
import com.iispl.util.SessionUtil;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;

/**
 * File    : com/iispl/composer/SidebarComposer.java
 * Purpose : Renders role-specific navigation items in the sidebar.
 *           Reads logged-in user's role from session and dynamically
 *           builds the appropriate menu. Highlights the active item.
 * ZUL     : component/sidebar.zul
 */
public class SidebarComposer extends SelectorComposer<Component> {

    @Wire private Div sidebarContainer;

    /** Current page path — used to highlight the active nav item. */
    private String currentPath;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);

        LoginDTO dto = SessionUtil.getCurrentUser();
        if (dto == null) return;

        currentPath = Executions.getCurrent().getDesktop().getRequestPath();

        // Clear any prior children (safety, in case of re-render)
        sidebarContainer.getChildren().clear();

        // Build nav based on role
        switch (dto.getRoleCode()) {
            case "ADMIN":           renderAdminNav();          break;
            case "MAKER_OUTWARD":   renderMakerOutwardNav();   break;
            case "CHECKER_OUTWARD": renderCheckerOutwardNav(); break;
            case "MAKER_INWARD":    renderMakerInwardNav();    break;
            case "CHECKER_INWARD":  renderCheckerInwardNav();  break;
            default: break;
        }
    }

    // ════════════════════════════════════════════
    //  Role-specific menus
    //  Pass NULL as targetUrl to render as disabled (not yet implemented)
    // ════════════════════════════════════════════

    private void renderAdminNav() {
        addSectionTitle("ADMINISTRATION");
        addNavItem("Dashboard",       "/admin/adminDashboard.zul");
        addNavItem("User Management", "/admin/userManagement/userManagement.zul");
        addNavItem("View Batches",    "/outward/viewBatches/viewBatches.zul");
        addNavItem("Reports",         null);
        addNavItem("Audit Logs",      null);
    }

    private void renderMakerOutwardNav() {
        addSectionTitle("OUTWARD");
        addNavItem("Dashboard",    "/dashboard/makerOutward/makerOutwardDashboard.zul");
        addNavItem("Batch Upload", "/outward/batchUpload/batchUpload.zul");
        addNavItem("MICR Repair",  "/outward/micrRepair/micrRepair.zul");
        addNavItem("Data Entry",   "/outward/acctAmount/acctAmount.zul");
        addNavItem("View Batches", "/outward/viewBatches/viewBatches.zul");
    }

    private void renderCheckerOutwardNav() {
        addSectionTitle("OUTWARD");
        addNavItem("Dashboard",           "/dashboard/checkerOutward/checkerOutwardDashboard.zul");
        addNavItem("Verification Queue",  "/outward/checkerQueue/checkerQueue.zul");
        addNavItem("DEM Export",          "/outward/demExport/demExport.zul");
        addNavItem("Reports",             "/reports/reports.zul");
    }

    private void renderMakerInwardNav() {
        addSectionTitle("INWARD");
        addNavItem("Dashboard",         "/dashboard/makerInward/makerInwardDashboard.zul");
        addNavItem("File Processing",   "/inward/bpxfUpload/bpxfUpload.zul");
        addNavItem("Inward Correction", "/inward/inwardMicr/inwardMicr.zul");
    }

    private void renderCheckerInwardNav() {
        addSectionTitle("INWARD");
        addNavItem("Dashboard",          "/dashboard/checkerInward/checkerInwardDashboard.zul");
        addNavItem("Verification Queue", null);  // Not yet implemented
        addNavItem("Reports",            null);  // Not yet implemented
    }

    // ════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════

    /** Creates a section title (e.g., "ADMINISTRATION", "OUTWARD"). */
    private void addSectionTitle(String title) {
        Div section = new Div();
        section.setSclass("nav-sec-title");
        section.appendChild(new Label(title));
        sidebarContainer.appendChild(section);
    }

    /**
     * Creates a single clickable navigation item.
     * If targetUrl is null → item is rendered as disabled (greyed out, no click).
     * If targetUrl matches currentPath → item is highlighted as active.
     */
    private void addNavItem(String label, String targetUrl) {
        boolean disabled = (targetUrl == null);
        boolean active   = !disabled && targetUrl.equals(currentPath);

        // Build the CSS class string
        StringBuilder sclass = new StringBuilder("nav-item");
        if (disabled) sclass.append(" disabled");
        if (active)   sclass.append(" active");

        Div item = new Div();
        item.setSclass(sclass.toString());

        // Icon wrapper
        Div iconWrap = new Div();
        iconWrap.setSclass("nav-icon");
        item.appendChild(iconWrap);

        // Label
        Label lbl = new Label(label);
        lbl.setSclass("nav-label");
        item.appendChild(lbl);

        // Click handler — only attached for enabled items
        if (!disabled) {
            final String url = targetUrl;
            item.addEventListener(Events.ON_CLICK, new EventListener<Event>() {
                @Override
                public void onEvent(Event event) {
                    Executions.sendRedirect(url);
                }
            });
        }

        sidebarContainer.appendChild(item);
    }
}