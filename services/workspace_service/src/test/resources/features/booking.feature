
Feature: Study space booking

  Scenario: Successfully book an available study space
    Given an available study space
    When a user books the study space
    Then the booking should be created successfully
    And the booking status should be pending

  Scenario: Reject overlapping bookings
    Given an existing booking for a study space
    When another user requests an overlapping booking
    Then the second booking should be rejected

  Scenario: Reject unavailable study spaces
    Given a study space that is unavailable
    When a user books the study space
    Then the booking should be rejected
