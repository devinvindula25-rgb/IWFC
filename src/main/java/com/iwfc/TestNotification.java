package com.iwfc;

import com.iwfc.facade.IWFCSystemFacade;

public class TestNotification {
    public static void main(String[] args) {
        IWFCSystemFacade facade = new IWFCSystemFacade();
        facade.login("ADM001");
        System.out.println("Admin notifications: " + facade.getNotifications().size());
        
        facade.login("MEM001");
        System.out.println("Member notifications: " + facade.getNotifications().size());
        
        facade.login("INS001");
        System.out.println("Instructor notifications: " + facade.getNotifications().size());
    }
}
