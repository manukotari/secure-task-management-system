package com.taskmanagement.secure.repository;

import com.taskmanagement.secure.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT u FROM Team t JOIN t.members u WHERE t.id = :teamId AND u.active = true")
    List<User> findActiveMembersByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT DISTINCT u FROM Team t JOIN t.members u WHERE t.manager.id = :managerId AND u.active = true")
    List<User> findActiveMembersByManagerId(@Param("managerId") Long managerId);

    @Query("SELECT u FROM User u WHERE u.active = true")
    List<User> findAllActiveUsers();
}
