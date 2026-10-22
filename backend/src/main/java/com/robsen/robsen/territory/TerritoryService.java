package com.robsen.robsen.territory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TerritoryService {
    private final TerritoryRepo territoryRepo;

    public TerritoryService(TerritoryRepo territoryRepo) {
        this.territoryRepo = territoryRepo;
    }

    @Transactional
    public Territory create(Territory territory) {
        territory.setId(null);
        return territoryRepo.save(territory);
    }

    @Transactional(readOnly = true)
    public Territory get(Long id) {
        return territoryRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Territory not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Territory> getAll() {
        return territoryRepo.findAll();
    }

    @Transactional
    public Territory update(Long id, Territory input) {
        Territory territory = get(id);
        territory.setName(input.getName());
        territory.setRegion(input.getRegion());
        territory.setCity(input.getCity());
        territory.setDescription(input.getDescription());
        return territoryRepo.save(territory);
    }

    @Transactional
    public void delete(Long id) {
        territoryRepo.delete(get(id));
    }
}
