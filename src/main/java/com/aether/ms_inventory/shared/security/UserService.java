package com.aether.ms_inventory.shared.security;

import com.aether.ms_inventory.shared.exceptions.UnauthorizedException;
import com.aether.ms_inventory.shared.helpers.CustomUserDetails;
import com.aether.ms_inventory.shared.persistence.postgres.entities.EmployeeEntity;
import com.aether.ms_inventory.shared.persistence.postgres.repositories.EmployeeRepository;
import com.aether.ms_inventory.shared.services.MessageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

  private final EmployeeRepository employeeRepository;
  private final MessageService messageService;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String email) {
    EmployeeEntity employee =  employeeRepository.findByEmail(email).orElseThrow(
        () -> new UnauthorizedException("exception.login.invalid")
    );

    List<SimpleGrantedAuthority> authorities = employee.getPermissionGroups()
        .stream()
        .flatMap(
            group -> group.getPermissions().stream()
        ).distinct()
        .map(
            permission -> new SimpleGrantedAuthority(permission.getName())
        ).toList();

    return new CustomUserDetails(
        employee.getEmail(),
        employee.getPasswordHash(),
        authorities,
        employee.getId()
    );
  }
}
