package com.robsen.robsen.territory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.robsen.robsen.security.BusinessAccessService;

@Service
public class TerritoryService {
    private final TerritoryRepo territoryRepo;
    private final BusinessAccessService businessAccess;

    public TerritoryService(TerritoryRepo territoryRepo, BusinessAccessService businessAccess) {
        this.territoryRepo = territoryRepo;
        this.businessAccess = businessAccess;
    }

    @Transactional
    public Territory create(Territory territory) {
        territory.setId(null);
        requireBusinessPermission(territory.getBusiness(), "territories:CREATE");
        return territoryRepo.save(territory);
    }

    @Transactional(readOnly = true)
    public Territory get(Long id) {
        Territory territory = territoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Territory not found: " + id));
        requireBusinessPermission(territory.getBusiness(), "territories:READ");
        return territory;
    }

    @Transactional(readOnly = true)
    public List<Territory> getAll() {
        if (businessAccess.isSuperAdmin()) return territoryRepo.findAll();
        List<Long> businessIds = businessAccess.accessibleBusinessIds();
        if (businessIds.isEmpty()) return List.of();
        return territoryRepo.findByBusinessIdIn(businessIds).stream()
                .filter(territory -> territory.getBusiness() != null
                        && businessAccess.canAccess(territory.getBusiness().getId(), "territories:READ"))
                .toList();
    }

    @Transactional
    public Territory update(Long id, Territory input) {
        Territory territory = get(id);
        requireBusinessPermission(territory.getBusiness(), "territories:UPDATE");
        requireBusinessPermission(input.getBusiness(), "territories:UPDATE");
        territory.setBusiness(input.getBusiness());
        territory.setName(input.getName());
        territory.setRegion(input.getRegion());
        territory.setCity(input.getCity());
        territory.setDescription(input.getDescription());
        return territoryRepo.save(territory);
    }

    @Transactional
    public void delete(Long id) {
        Territory territory = get(id);
        requireBusinessPermission(territory.getBusiness(), "territories:DELETE");
        territoryRepo.delete(territory);
    }

    private void requireBusinessPermission(com.robsen.robsen.business.Business business, String permission) {
        if (business == null) {
            if (!businessAccess.isSuperAdmin()) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Territories must belong to an accessible business.");
            }
            return;
        }
        businessAccess.requireAccess(business.getId(), permission);
    }
}
