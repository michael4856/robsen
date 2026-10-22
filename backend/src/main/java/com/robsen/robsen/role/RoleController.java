package com.robsen.robsen.role;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController 
@RequestMapping ("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService){
        this.roleService = roleService;
    }
    
    @GetMapping ("/{id}")
    public Role getRoleById(@PathVariable Long id){
        return roleService.getRoleById(id);
     }
    @GetMapping("/getRole")
    public Role getRoleByLegacyQuery(@org.springframework.web.bind.annotation.RequestParam Long id) {
       return roleService.getRoleById(id);
     }
    @GetMapping({"", "/all"})
    public List<Role> getAllRoles(){
       return roleService.getAllRoles();
    }
     @GetMapping("/active")
     public List<Role> getAllActiveRoles() {
        return roleService.getAllActiveRoles();
     }
     @PostMapping({"", "/create"})
     @org.springframework.web.bind.annotation.ResponseStatus(HttpStatus.CREATED)
     public Role createRole(@RequestBody Role role){
        return roleService.createRole(role);
     }
     @PutMapping("/{id}")
     public Role updateRole(@PathVariable Long id, @RequestBody Role updatedRole){
        return roleService.updateRole(id, updatedRole); 
     }
     @PutMapping("/update")
     public Role updateRoleByLegacyQuery(
             @org.springframework.web.bind.annotation.RequestParam Long id,
             @RequestBody Role updatedRole) {
        return roleService.updateRole(id, updatedRole);
     }
     @DeleteMapping("/{id}")
     public ResponseEntity<Void> deleteRole(@PathVariable Long id){
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
        }
     @DeleteMapping("/delete")
     public ResponseEntity<Void> deleteRoleByLegacyQuery(
             @org.springframework.web.bind.annotation.RequestParam Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
     }
}
