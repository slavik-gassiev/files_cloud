package com.slava.service;

import com.slava.entity.Role;
import com.slava.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    public Role saveRole(Role role) {
        return roleRepository.save(role);
    }

    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    @PostConstruct
    public void initRoles() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(new Role("ROLE_USER"));
        }
        if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
            roleRepository.save(new Role("ROLE_ADMIN"));
        }
    }
}
