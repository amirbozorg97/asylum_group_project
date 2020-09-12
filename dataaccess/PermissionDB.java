package com.asylumproject.asylumproject.dataaccess;

import com.asylumproject.asylumproject.permission.Permission;
import com.asylumproject.asylumproject.permission.PermissionName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;

@EnableJpaRepositories
public interface PermissionDB extends JpaRepository<Permission, Long> {

    /**
     * retrieve a permission object from database.
     *
     * @param permissionName the permission name object that is used to get a Permission object.
     * @return it returns a permission object.
     */
    Optional<Permission> findByName(PermissionName permissionName);
}
