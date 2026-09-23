package com.iwfc.pattern;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.model.Equipment;
import com.iwfc.model.EquipmentStatus;
import com.iwfc.model.Location;
import com.iwfc.model.Payment;
import com.iwfc.model.PaymentMethod;
import com.iwfc.model.PaymentPurpose;
import com.iwfc.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    private Repository<Equipment> equipmentRepository;
    private Repository<Payment> paymentRepository;

    @BeforeEach
    void setUp() {
        equipmentRepository = new Repository<>();
        paymentRepository = new Repository<>();
    }

    @Test
    void testRepositoryWithEquipmentType() throws DuplicateDataException {
        Equipment eq = new Equipment("EQ1", "Treadmill", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 100.0, 500.0);
        
        // Save
        equipmentRepository.save(eq);
        assertEquals(1, equipmentRepository.findAll().size());
        
        // Find
        Optional<Equipment> found = equipmentRepository.findById("EQ1");
        assertTrue(found.isPresent());
        assertEquals("Treadmill", found.get().getName());
        
        // Delete
        equipmentRepository.delete("EQ1");
        assertEquals(0, equipmentRepository.findAll().size());
    }

    @Test
    void testRepositoryWithPaymentType() throws DuplicateDataException {
        Payment pay = new Payment("PAY1", "MEM1", 1000.0, 25.0, PaymentMethod.CARD, PaymentPurpose.MEMBERSHIP_FEE, "Monthly Dues");
        
        // Save
        paymentRepository.save(pay);
        assertEquals(1, paymentRepository.findAll().size());
        
        // Find
        Optional<Payment> found = paymentRepository.findById("PAY1");
        assertTrue(found.isPresent());
        assertEquals(1000.0, found.get().getAmount());
        
        // Delete
        paymentRepository.delete("PAY1");
        assertEquals(0, paymentRepository.findAll().size());
    }

    @Test
    void testSaveDuplicateThrowsException() {
        Equipment eq = new Equipment("EQ1", "Treadmill", "Cardio", EquipmentStatus.OPERATIONAL, Location.CARDIO_ZONE, 100.0, 500.0);
        
        assertDoesNotThrow(() -> equipmentRepository.save(eq));
        
        DuplicateDataException ex = assertThrows(DuplicateDataException.class, 
            () -> equipmentRepository.save(eq));
            
        assertTrue(ex.getMessage().contains("An item with ID EQ1 already exists."));
    }
}
