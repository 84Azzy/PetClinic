package com.zzy.petclinic.pet;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zzy.petclinic.rbac.authentication.AuthenticatedUser;
import com.zzy.petclinic.rbac.authentication.SysUser;
import com.zzy.petclinic.rbac.authorization.UserAuthorities;
import com.zzy.petclinic.common.PageQuery;
import com.zzy.petclinic.common.PageResponse;
import com.zzy.petclinic.owner.Owner;
import com.zzy.petclinic.owner.OwnerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

class PetControllerTest {
  private static final PageQuery QUERY = new PageQuery(1L, 10L, null, null);

  private PetController controller;
  private PetService petService;
  private OwnerService ownerService;

  @BeforeEach
  void setUp() {
    controller = new PetController();
    petService = mock(PetService.class);
    ownerService = mock(OwnerService.class);
    ReflectionTestUtils.setField(controller, "petService", petService);
    ReflectionTestUtils.setField(controller, "ownerService", ownerService);
  }

  @Test
  void missingPrincipalIsUnauthorized() {
    assertThrows(
        AuthenticationCredentialsNotFoundException.class,
        () -> controller.page(QUERY, null, null));
    verify(petService, never()).page(QUERY, null);
  }

  @Test
  void adminCanQueryAllOwners() {
    PageResponse<Pet> expected = emptyPage();
    when(petService.page(QUERY, null)).thenReturn(expected);

    assertSame(expected, controller.page(QUERY, null, user(1L, "ADMIN")).data());
    verify(petService).page(QUERY, null);
  }

  @Test
  void staffCanFilterByOwner() {
    PageResponse<Pet> expected = emptyPage();
    when(petService.page(QUERY, 2L)).thenReturn(expected);

    assertSame(expected, controller.page(QUERY, 2L, user(2L, "STAFF")).data());
    verify(petService).page(QUERY, 2L);
  }

  @Test
  void ownerWithoutFilterIsRestrictedToOwnPets() {
    Owner owner = owner(3L, 10L);
    when(ownerService.mine()).thenReturn(owner);
    PageResponse<Pet> expected = emptyPage();
    when(petService.page(QUERY, 10L)).thenReturn(expected);

    assertSame(expected, controller.page(QUERY, null, user(3L, "OWNER")).data());
    verify(petService).page(QUERY, 10L);
  }

  @Test
  void ownerCanExplicitlyFilterByOwnOwnerId() {
    Owner owner = owner(3L, 10L);
    when(ownerService.mine()).thenReturn(owner);

    controller.page(QUERY, 10L, user(3L, "OWNER"));

    verify(petService).page(QUERY, 10L);
  }

  @Test
  void ownerCannotQueryAnotherOwnersPets() {
    when(ownerService.mine()).thenReturn(owner(3L, 10L));

    assertThrows(
        AccessDeniedException.class,
        () -> controller.page(QUERY, 11L, user(3L, "OWNER")));
    verify(petService, never()).page(QUERY, 11L);
  }

  @Test
  void unsupportedAccountTypeIsForbidden() {
    assertThrows(
        AccessDeniedException.class,
        () -> controller.page(QUERY, null, user(5L, "UNKNOWN")));
    verify(petService, never()).page(QUERY, null);
  }

  private static AuthenticatedUser user(Long id, String accountType) {
    SysUser user = new SysUser();
    user.setId(id);
    user.setUsername("user-" + id);
    user.setPasswordHash("password");
    user.setAccountType(accountType);
    user.setStatus("ACTIVE");
    return new AuthenticatedUser(user, UserAuthorities.empty());
  }

  private static Owner owner(Long userId, Long ownerId) {
    Owner owner = new Owner();
    owner.setId(ownerId);
    owner.setUserId(userId);
    return owner;
  }

  private static PageResponse<Pet> emptyPage() {
    return new PageResponse<>(List.of(), 0, 1, 10);
  }
}
