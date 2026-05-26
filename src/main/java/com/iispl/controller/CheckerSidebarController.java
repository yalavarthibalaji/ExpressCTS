/*this checker module sidebar function implementation created by ramana*/
package com.iispl.controller;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Div;

// Controls components/checkerSidebar.zul
// Pattern: identical to MakerSidebarController — @Listen("onClick = #navXxx")
public class CheckerSidebarController extends SelectorComposer<Component> {

    @Wire("#navDashboard")  private Div navDashboard;
    @Wire("#navBatchMgmt")  private Div navBatchMgmt;
    @Wire("#navMicrRepair") private Div navMicrRepair;
    @Wire("#navChecker")    private Div navChecker;
    @Wire("#navReports")    private Div navReports;
    @Wire("#navAudit")      private Div navAudit;

    private Div[] allNavItems;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        allNavItems = new Div[]{ navDashboard, navBatchMgmt, navMicrRepair,
                                 navChecker, navReports, navAudit };
        setActive(navChecker); // default: Checker Verification is active on load
    }

    // Direct onClick listeners — same pattern as MakerSidebarController
    @Listen("onClick = #navDashboard")
    public void onDashboard(Event e)  { setActive(navDashboard);  fireNav("onNavDashboard"); }

    @Listen("onClick = #navBatchMgmt")
    public void onBatchMgmt(Event e)  { setActive(navBatchMgmt);  fireNav("onNavBatchMgmt"); }

    @Listen("onClick = #navMicrRepair")
    public void onMicrRepair(Event e) { setActive(navMicrRepair); fireNav("onNavMicrRepair"); }

    @Listen("onClick = #navChecker")
    public void onChecker(Event e)    { setActive(navChecker);    fireNav("onNavChecker"); }

    @Listen("onClick = #navReports")
    public void onReports(Event e)    { setActive(navReports);    fireNav("onNavReports"); }

    @Listen("onClick = #navAudit")
    public void onAudit(Event e)      { setActive(navAudit);      fireNav("onNavAudit"); }

    // Remove active from all nav items, add to clicked one
    private void setActive(Div target) {
        for (Div item : allNavItems) {
            if (item != null) {
                item.setSclass("cts-nav-item" + (item == target ? " active" : ""));
            }
        }
    }

    // FIX: Fire nav event directly on #chkApp — the root div where CheckerController lives.
    // Previous code walked to the page root (<zk>) which is NOT #chkApp,
    // so @Listen("onNavXxx = #chkApp") never received the event.
    // getPage().getFellow("chkApp") finds it correctly across include boundaries.
    private void fireNav(String eventName) {
        org.zkoss.zk.ui.Component chkApp = getSelf().getPage().getFellow("chkApp");
        if (chkApp != null) {
            Events.postEvent(new Event(eventName, chkApp));
        }
    }
}