package com.showcase.feed.auth;

import com.showcase.feed.common.bloom.UsernameBloomFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UsernameAvailabilityServiceTest {

    @Test
    void bloomFilterDefiniteNo_shortCircuitsWithoutHittingTheRepository() {
        UsernameBloomFilter bloomFilter = mock(UsernameBloomFilter.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(bloomFilter.mightContain("newuser")).thenReturn(false);

        UsernameAvailabilityService service = new UsernameAvailabilityService(bloomFilter, userRepository);

        assertTrue(service.isAvailable("newuser"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void bloomFilterMaybe_fallsBackToRepositoryCheck() {
        UsernameBloomFilter bloomFilter = mock(UsernameBloomFilter.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(bloomFilter.mightContain("abhay")).thenReturn(true);
        when(userRepository.existsByUsername("abhay")).thenReturn(true);

        UsernameAvailabilityService service = new UsernameAvailabilityService(bloomFilter, userRepository);

        assertFalse(service.isAvailable("abhay"));
        verify(userRepository).existsByUsername("abhay");
    }
}
