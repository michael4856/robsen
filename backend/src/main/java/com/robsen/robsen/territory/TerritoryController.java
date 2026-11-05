package com.robsen.robsen.territory;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/territories")
public class TerritoryController {
    private final TerritoryService territoryService;

    public TerritoryController(TerritoryService territoryService) {
        this.territoryService = territoryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Territory create(@RequestBody Territory territory) {
        return territoryService.create(territory);
    }

    @GetMapping("/{id}")
    public Territory get(@PathVariable Long id) {
        return territoryService.get(id);
    }

    @GetMapping
    public List<Territory> getAll() {
        return territoryService.getAll();
    }

    @PutMapping("/{id}")
    public Territory update(@PathVariable Long id, @RequestBody Territory territory) {
        return territoryService.update(id, territory);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        territoryService.delete(id);
    }
}
