package com.iwfc.service;

import com.iwfc.model.Admin;
import com.iwfc.model.Instructor;
import com.iwfc.model.Member;
import com.iwfc.model.User;
import com.iwfc.pattern.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAuthenticationTest {

    private UserService userService;

    @BeforeEach
    void setUp() {
        DataStore.resetInstance();
        userService = new UserService();
    }

    @Test
    void testDefaultPasscode0000ForAllRoles() {
        User admin = DataStore.getInstance().getUsers().findById("ADM001").orElse(null);
        User instructor = DataStore.getInstance().getUsers().findById("INS001").orElse(null);
        User member = DataStore.getInstance().getUsers().findById("MEM001").orElse(null);

        assertNotNull(admin);
        assertNotNull(instructor);
        assertNotNull(member);

        assertEquals("0000", admin.getPassword(), "Admin default passcode must be 0000");
        assertEquals("0000", instructor.getPassword(), "Instructor default passcode must be 0000");
        assertEquals("0000", member.getPassword(), "Member default passcode must be 0000");
    }

    @Test
    void testSuccessfulLoginWith0000() {
        User admin = userService.login("ADM001", "0000");
        assertNotNull(admin);
        assertTrue(admin instanceof Admin);
        assertEquals("ADM001", admin.getUserId());

        User instructor = userService.login("INS001", "0000");
        assertNotNull(instructor);
        assertTrue(instructor instanceof Instructor);
        assertEquals("INS001", instructor.getUserId());

        User member = userService.login("MEM001", "0000");
        assertNotNull(member);
        assertTrue(member instanceof Member);
        assertEquals("MEM001", member.getUserId());
    }

    @Test
    void testLoginWithWrongPasswordThrowsException() {
        IllegalArgumentException adminEx = assertThrows(IllegalArgumentException.class,
                () -> userService.login("ADM001", "9999"));
        assertTrue(adminEx.getMessage().contains("Invalid password") || adminEx.getMessage().contains("passcode"));

        IllegalArgumentException memberEx = assertThrows(IllegalArgumentException.class,
                () -> userService.login("MEM001", "wrong_passcode"));
        assertTrue(memberEx.getMessage().contains("Invalid password") || memberEx.getMessage().contains("passcode"));
    }

    @Test
    void testPasscodeUpdateAndAuthentication() {
        User member = DataStore.getInstance().getUsers().findById("MEM001").orElseThrow();
        member.setPassword("5678");
        DataStore.getInstance().getUsers().update(member);

        // Old passcode should fail
        assertThrows(IllegalArgumentException.class, () -> userService.login("MEM001", "0000"));

        // New passcode should succeed
        User authenticated = userService.login("MEM001", "5678");
        assertNotNull(authenticated);
        assertEquals("MEM001", authenticated.getUserId());
    }

    @Test
    void testNonExistentUserLoginThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> userService.login("UNKNOWN_USER_999", "0000"));
    }
}
