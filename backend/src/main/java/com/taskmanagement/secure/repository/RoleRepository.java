package com.taskmanagement.secure.repository;

import com.taskmanagement.secure.entity.Role;
import com.taskmanagement.secure.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(RoleName name);
}
