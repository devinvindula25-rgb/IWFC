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
import com.iwfc.model.Member;
import com.iwfc.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionVerificationTest {

    private IWFCSystemFacade facade;

    @BeforeEach
    void setUp() {
        facade = new IWFCSystemFacade();
        UserService userService = new UserService();
        try {
            userService.addUser(new Admin("ADM_TEST", "Admin Test", "admin@test.com", "1234567890", "pic", "pwd"));
            userService.addUser(new Member("MEM_TEST", "Member Test", "mem@test.com", "1234567890", "pic", "pwd", "Silver"));
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
}
