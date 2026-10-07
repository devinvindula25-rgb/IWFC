package com.iwfc.exception;

import com.iwfc.facade.IWFCSystemFacade;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.Location;
import com.iwfc.model.PaymentMethod;
import com.iwfc.model.PaymentPurpose;
import com.iwfc.pattern.payment.PaymentContext;
import com.iwfc.service.MockPaymentGateway;
import com.iwfc.model.Admin;
import com.iwfc.model.Instructor;
import com.iwfc.model.Member;
import com.iwfc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionVerificationTest {

    private IWFCSystemFacade facade;

    @BeforeEach
    void setUp() {
        com.iwfc.pattern.DataStore.resetInstance();
        facade = new IWFCSystemFacade();
        UserService userService = new UserService();
        try {
            userService.addUser(new Admin("ADM_TEST", "Admin Test", "admin@test.com", "1234567890", "pic", "pwd"));
            userService.addUser(new Member("MEM_TEST", "Member Test", "mem@test.com", "1234567890", "pic", "pwd", "Silver"));
            userService.addUser(new Instructor("INS_TEST", "Instructor Test", "ins@test.com", "1234567890", "pic", "pwd", "Yoga"));
        } catch (Exception ignored) {}
    }

    @Test
    void testUnauthorizedAccessException() {
        // Login as Member (lowest privilege)
        facade.login("MEM_TEST");

        // Attempt admin action (MANAGE_EQUIPMENT)
        Equipment equipment = new Equipment("EQ999", "Bench", "Weights", EquipmentStatus.OPERATIONAL, Location.WEIGHT_ROOM, 100.0, 500.0);
        
        UnauthorizedAccessException ex = assertThrows(UnauthorizedAccessException.class, 
            () -> facade.addEquipment(equipment));
            
        assertEquals("Access denied: User 'Member Test' does not have permission 'MANAGE_EQUIPMENT'.", ex.getMessage());
    }

    @Test
    void testDuplicateDataException() {
        // Login as Admin
        facade.login("ADM_TEST");

        Equipment equipment1 = new Equipment("EQ1", "Treadmill", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 100.0, 500.0);
        Equipment equipment2 = new Equipment("EQ1", "Elliptical", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 100.0, 500.0);

        try {
            facade.addEquipment(equipment1);
        } catch (Exception e) {
            fail("First insertion should succeed");
        }

        // Attempting to add equipment with the same ID
        DuplicateDataException ex = assertThrows(DuplicateDataException.class, 
            () -> facade.addEquipment(equipment2));
            
        assertTrue(ex.getMessage().contains("An item with ID EQ1 already exists."));
    }

    @Test
    void testPaymentFailedExceptionAndInsufficientFunds() {
        // Login as Admin
        facade.login("ADM_TEST");

        // Force the mock gateway to fail
        MockPaymentGateway.getInstance().setForceFailureMode(true);

        PaymentContext ctx = new PaymentContext();
        ctx.setCardNumber("1234567812345678");
        ctx.setExpiryDate("12/25");

        PaymentFailedException ex = assertThrows(PaymentFailedException.class, 
            () -> facade.processPayment("MEM1", 1000.0, PaymentMethod.CARD, PaymentPurpose.MEMBERSHIP_FEE, "Test", ctx));
            
        // Because InsufficientFunds extends PaymentFailed, it gets caught here.
        // We verify the actual instance is InsufficientFundsException.
        assertTrue(ex instanceof InsufficientFundsException);
        assertEquals("PAY-002", ex.getMessage());

        // Reset state
        MockPaymentGateway.getInstance().setForceFailureMode(false);
    }

    @Test
    void testInvalidBookingException() {
        // 1. Member attempting to book a session on an invalid / inactive date
        facade.login("MEM_TEST");
        InvalidBookingException exDate = assertThrows(InvalidBookingException.class, 
            () -> facade.bookSessionForDate("SES001", java.time.LocalDate.now().plusDays(100)));
        assertNotNull(exDate.getMessage());
        assertTrue(exDate.getMessage().contains("is not active on") || exDate.getMessage().contains("Cannot book"));

        // 2. Instructor attempting to set session hours outside operating facility hours (before 06:00)
        facade.login("INS_TEST");
        InvalidBookingException exSlot = assertThrows(InvalidBookingException.class,
            () -> facade.editSession("SES001", "Morning HIIT", java.time.LocalTime.of(5, 0), java.time.LocalTime.of(6, 0), 20));
        assertTrue(exSlot.getMessage().contains("Session times must be within operating hours"));
    }
}
