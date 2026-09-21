package com.hms.dto.request;

/**
 * Validation group for constraints that only make sense when a record is first
 * created.
 *
 * A booking cannot be made for a date that has already passed, but an
 * appointment that has passed still has to be editable - marking yesterday's
 * appointment COMPLETED is the most routine thing a receptionist does. Putting
 * the date constraint in this group means it is checked on POST and skipped on
 * PUT.
 */
public interface OnCreate {
}
