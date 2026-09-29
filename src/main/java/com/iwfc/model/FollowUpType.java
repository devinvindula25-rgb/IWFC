package com.iwfc.model;

public enum FollowUpType {
    ENQUIRY("Enquiry"),
    BALANCE_DUE("Balance Due"),
    FEEDBACK("Feedback"),
    TRIAL("Trial"),
    MEMBERSHIP_RENEWAL("Membership Renewal"),
    BIRTHDAY("Birthday"),
    ANNIVERSARY("Anniversary"),
    PARTY("Party");

    private final String displayName;

    FollowUpType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
