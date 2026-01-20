package com.kuberfashion.backend.service;

import com.kuberfashion.backend.entity.Address;
import com.kuberfashion.backend.entity.User;
import com.kuberfashion.backend.exception.ResourceNotFoundException;
import com.kuberfashion.backend.repository.AddressRepository;
import com.kuberfashion.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
    }

    public Address getAddressById(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));
    }

    public Address getDefaultAddress(Long userId) {
        return addressRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
    }

    public Address createAddress(Long userId, Address addressData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address address = new Address();
        address.setUser(user);
        copyAddressFields(addressData, address);

        // If this is the first address or explicitly set as default, make it default
        if (addressData.isDefault() || addressRepository.countByUserId(userId) == 0) {
            addressRepository.clearDefaultForUser(userId, 0L);
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    public Address updateAddress(Long addressId, Long userId, Address addressData) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        copyAddressFields(addressData, address);

        // Handle default address logic
        if (addressData.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(userId, addressId);
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    public void setAsDefault(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        addressRepository.clearDefaultForUser(userId, addressId);
        address.setDefault(true);
        addressRepository.save(address);
    }

    public void deleteAddress(Long addressId, Long userId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found with id: " + addressId));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        // If deleted address was default, set another as default
        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId);
            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    private void copyAddressFields(Address source, Address target) {
        target.setLabel(source.getLabel());
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setPhone(source.getPhone());
        target.setAddressLine1(source.getAddressLine1());
        target.setAddressLine2(source.getAddressLine2());
        target.setCity(source.getCity());
        target.setState(source.getState());
        target.setPostalCode(source.getPostalCode());
        target.setCountry(source.getCountry());
    }
}
