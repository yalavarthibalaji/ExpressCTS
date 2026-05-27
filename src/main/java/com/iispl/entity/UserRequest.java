package com.iispl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * UserRequest entity — maps to user_requests table. Every admin action (add,
 * delete, enable, disable, update, changeRole, resetPwd) is saved here first as
 * a PENDING request. Another admin can then Accept or Decline it.
 */
@Entity
@Table(name = "user_requests")
public class UserRequest {

	@Id
	@Column(name = "request_id")
	private String requestId; // e.g. "REQ001"

	// Type of request: ADD_USER, DELETE_USER, ENABLE_USER,
	// DISABLE_USER, UPDATE_USER, CHANGE_ROLE, RESET_PASSWORD
	@Column(name = "request_type")
	private String requestType;

	@Column(name = "target_user_id")
	private String targetUserId; // user this request is about

	@Column(name = "requested_by")
	private String requestedBy; // admin who raised this request

	@Column(name = "details")
	private String details; // human-readable summary of the change

	// Status: PENDING, ACCEPTED, DECLINED
	@Column(name = "status")
	private String status;

	@Column(name = "request_date")
	private String requestDate; // stored as string e.g. "2025-05-25"

	@Column(name = "actioned_by")
	private String actionedBy; // admin who accepted or declined
	
	public UserRequest(){}

	// getters and setters

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String r) {
		this.requestId = r;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String r) {
		this.requestType = r;
	}

	public String getTargetUserId() {
		return targetUserId;
	}

	public void setTargetUserId(String t) {
		this.targetUserId = t;
	}

	public String getRequestedBy() {
		return requestedBy;
	}

	public void setRequestedBy(String r) {
		this.requestedBy = r;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String d) {
		this.details = d;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String s) {
		this.status = s;
	}

	public String getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(String d) {
		this.requestDate = d;
	}

	public String getActionedBy() {
		return actionedBy;
	}

	public void setActionedBy(String a) {
		this.actionedBy = a;
	}
}