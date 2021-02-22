package org.folio.circulation.resources.handlers.error;

public enum CirculationErrorType {
  // Errors related to the service point
  INVALID_PICKUP_SERVICE_POINT,
  SERVICE_POINT_IS_NOT_PRESENT,

  // Errors related to the item
  INVALID_ITEM,
  INVALID_STATUS,
  INVALID_ITEM_ID,
  ITEM_DOES_NOT_EXIST,
  ITEM_ALREADY_REQUESTED_BY_SAME_USER,
  ITEM_ALREADY_LOANED_TO_SAME_USER,
  ITEM_REQUESTED_BY_ANOTHER_PATRON,
  ITEM_IS_NOT_LOANABLE,
  ITEM_IS_NOT_ALLOWED_FOR_CHECK_OUT,
  ITEM_ALREADY_CHECKED_OUT,
  ITEM_HAS_OPEN_LOANS,
  ITEM_LIMIT_IS_REACHED,

  // Errors related to the loan
  FAILED_TO_FIND_SINGLE_OPEN_LOAN,

  // Errors that represent fetch failures
  FAILED_TO_FETCH_ITEM,
  FAILED_TO_FETCH_USER,
  FAILED_TO_FETCH_PROXY_USER,

  // Errors related to the user and relationship between the user and the proxy user
  USER_IS_INACTIVE,
  PROXY_USER_IS_INACTIVE,
  USER_IS_BLOCKED_MANUALLY,
  USER_IS_BLOCKED_AUTOMATICALLY,
  INVALID_USER_OR_PATRON_GROUP_ID,
  INVALID_PROXY_RELATIONSHIP,
  USER_DOES_NOT_MATCH,

  // Errors related to requests
  REQUESTING_DISALLOWED,
  REQUESTING_DISALLOWED_BY_POLICY,

  // Errors related ro renewal
  RENEWAL_VALIDATION_ERROR,

  // Errors related to block overrides
  INSUFFICIENT_OVERRIDE_PERMISSIONS
}
