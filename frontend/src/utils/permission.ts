export type Role = "ADMIN" | "STAFF" | "OWNER";

export interface AccessRequirement {
  roles?: Role[];
  authorities?: string[];
  mode?: "all" | "any";
}

export const canAccess = (
  currentAuthorities: readonly string[],
  requirement: AccessRequirement,
): boolean => {
  const roles = requirement.roles ?? [];
  const authorities = requirement.authorities ?? [];
  const mode = requirement.mode ?? "all";

  const roleAllowed =
    roles.length === 0 ||
    roles.some((role) => currentAuthorities.includes(`ROLE_${role}`));

  const authorityAllowed =
    authorities.length === 0 ||
    (mode === "all"
      ? authorities.every((code) => currentAuthorities.includes(code))
      : authorities.some((code) => currentAuthorities.includes(code)));

  return roleAllowed && authorityAllowed;
};
