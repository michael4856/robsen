package com.robsen.robsen.role;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class RoleBootstrapTest {
    private final RoleRepo roleRepo = mock(RoleRepo.class);

    @Test
    void persistsAllDefaultRolesWhenDatabaseHasNone() {
        when(roleRepo.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());

        new RoleBootstrap(roleRepo).run();

        verify(roleRepo, times(11)).save(any(Role.class));
    }

    @Test
    void keepsExistingCustomizedRolesAndBackfillsEmptyRoles() {
        Role customizedRole = new Role();
        customizedRole.setName("OWNER");
        customizedRole.setPermissions(List.of("products:READ"));
        when(roleRepo.findByNameIgnoreCase("OWNER")).thenReturn(Optional.of(customizedRole));
        when(roleRepo.findByNameIgnoreCase(org.mockito.ArgumentMatchers.argThat(name -> !"OWNER".equals(name))))
                .thenReturn(Optional.empty());

        new RoleBootstrap(roleRepo).run();

        verify(roleRepo, never()).save(customizedRole);
        verify(roleRepo, times(10)).save(any(Role.class));
    }
}
