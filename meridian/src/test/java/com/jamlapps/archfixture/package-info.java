/**
 * A deliberately broken two-module application used only to prove that boundary verification
 * detects a violation. See {@code BoundaryEnforcementTests}.
 *
 * <p>It lives outside {@code com.jamlapps.meridian} on purpose. {@code ApplicationModules} scans
 * the package tree of whatever it is anchored on, so a fixture inside the application's package
 * would be picked up as real modules by {@code ModularityTests} and fail the build for the wrong
 * reason.
 */
package com.jamlapps.archfixture;
