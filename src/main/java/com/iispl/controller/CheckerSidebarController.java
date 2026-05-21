package com.iispl.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;

/**
 * CheckerSidebarController.java
 * ZK MVC Composer — controls components/checkerSidebar.zul
 *
 * Package : com.iispl.controller
 * Pattern : ZK MVC (SelectorComposer) — identical pattern to MakerSidebarController
 *
 * How it works (same as MakerSidebarController):
 *   1. Each nav item in checkerSidebar.zul has forward="onClick=onNavXxx"
 *   2. That fires onNavXxx on THIS composer's root component (the sidebar div)
 *   3. This controller's @Listen catches it, sets the active nav style,
 *      then calls fireNav() which walks up to the root cts-app div and
 *      posts the same event there
 *   4. CheckerController has @Listen("onNavXxx = div#chkApp") which catches it
 *      and switches the visible panel
 *
 * DO NOT change any nav event names — CheckerController @Listen annotations
 * already match: onNavDashboard, onNavBatchMgmt, onNavMicrRepair,
 * onNavChecker, onNavReports, onNavAudit
 */
public class CheckerSidebarController extends SelectorComposer<Component> {

    // ── Wired nav items ──────────────────────────────────────────────
    @Wire("#navDashboard")  private Div navDashboard;
    @Wire("#navBatchMgmt")  private Div navBatchMgmt;
    @Wire("#navMicrRepair") private Div navMicrRepair;
    @Wire("#navChecker")    private Div navChecker;
    @Wire("#navReports")    private Div navReports;
    @Wire("#navAudit")      private Div navAudit;

    private Div[] allNavItems;

    // ── Lifecycle ────────────────────────────────────────────────────

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        allNavItems = new Div[]{
            navDashboard, navBatchMgmt, navMicrRepair,
            navChecker, navReports, navAudit
        };
        // Checker (Verification) is the default active item
        setActive(navChecker);
    }

    // ── Click handlers — one per nav item ───────────────────────────

    @Listen("onNavDashboard = div")
    public void onDashboard(Event e) {
        setActive(navDashboard);
        fireNav("onNavDashboard");
    }

    @Listen("onNavBatchMgmt = div")
    public void onBatchMgmt(Event e) {
        setActive(navBatchMgmt);
        fireNav("onNavBatchMgmt");
    }

    @Listen("onNavMicrRepair = div")
    public void onMicrRepair(Event e) {
        setActive(navMicrRepair);
        fireNav("onNavMicrRepair");
    }

    @Listen("onNavChecker = div")
    public void onChecker(Event e) {
        setActive(navChecker);
        fireNav("onNavChecker");
    }

    @Listen("onNavReports = div")
    public void onReports(Event e) {
        setActive(navReports);
        fireNav("onNavReports");
    }

    @Listen("onNavAudit = div")
    public void onAudit(Event e) {
        setActive(navAudit);
        fireNav("onNavAudit");
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Remove "active" from all nav items, add it to the clicked one.
     * Same logic as MakerSidebarController.setActive().
     */
    private void setActive(Div target) {
        for (Div item : allNavItems) {
            if (item != null) {
                item.setSclass("cts-nav-item" + (item == target ? " active" : ""));
            }
        }
    }

    /**
     * Walk up to the root cts-app div and fire the event there.
     * CheckerController's @Listen annotations on "div#chkApp" will catch it.
     *
     * Identical logic to MakerSidebarController.fireNav().
     */
    private void fireNav(String eventName) {
        Component target = getSelf().getParent();
        while (target != null && target.getParent() != null) {
            target = target.getParent();
        }
        if (target == null) target = getSelf();
        Events.postEvent(new Event(eventName, target));
    }
}
