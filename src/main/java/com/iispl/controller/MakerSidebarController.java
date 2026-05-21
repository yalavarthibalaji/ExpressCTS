package com.iispl.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;

/**
 * MakerSidebarController.java
 * ZK MVC Composer — controls components/makerSidebar.zul
 *
 * Each nav item click fires a custom event on the sidebar's parent page
 * (the cts-app div in makerDashboard.zul), which the
 * MakerDashboardController listens for.
 *
 * Active state: removes "active" from all nav items, adds it to clicked one.
 */
public class MakerSidebarController extends SelectorComposer<Component> {

    @Wire("#navDashboard")    private Div navDashboard;
    @Wire("#navCreateBatch")  private Div navCreateBatch;
    @Wire("#navLoadCheques")  private Div navLoadCheques;
    @Wire("#navMakerQueue")   private Div navMakerQueue;
    @Wire("#navMicrRepair")   private Div navMicrRepair;
    @Wire("#navReports")      private Div navReports;

    private Div[] allNavItems;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        allNavItems = new Div[]{
            navDashboard, navCreateBatch, navLoadCheques,
            navMakerQueue, navMicrRepair, navReports
        };
    }

    @Listen("onClick = #navDashboard")
    public void onDashboard(Event e) {
        setActive(navDashboard);
        fireNav("onNavDashboard");
    }

    @Listen("onClick = #navCreateBatch")
    public void onCreateBatch(Event e) {
        setActive(navCreateBatch);
        fireNav("onNavCreateBatch");
    }

    @Listen("onClick = #navLoadCheques")
    public void onLoadCheques(Event e) {
        setActive(navLoadCheques);
        fireNav("onNavLoadCheques");
    }

    @Listen("onClick = #navMakerQueue")
    public void onMakerQueue(Event e) {
        setActive(navMakerQueue);
        fireNav("onNavMakerQueue");
    }

    @Listen("onClick = #navMicrRepair")
    public void onMicrRepair(Event e) {
        setActive(navMicrRepair);
        fireNav("onNavMicrRepair");
    }

    @Listen("onClick = #navReports")
    public void onReports(Event e) {
        setActive(navReports);
        fireNav("onNavReports");
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private void setActive(Div target) {
        for (Div item : allNavItems) {
            if (item != null) {
                item.setSclass("cts-nav-item" + (item == target ? " active" : ""));
            }
        }
    }

    /**
     * Fire a custom event that bubbles up to the root cts-app div
     * where MakerDashboardController's @Listen annotations catch it.
     */
    private void fireNav(String eventName) {
        // Walk up to the cts-app root and fire there
        Component target = getSelf().getParent();
        while (target != null && target.getParent() != null) {
            target = target.getParent();
        }
        if (target == null) target = getSelf();
        Events.postEvent(new Event(eventName, target));
    }
}