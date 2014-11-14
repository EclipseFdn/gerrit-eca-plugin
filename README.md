gerrit-cla-plugin
=================

A Gerrit plugin for controlling pushes to eclipse.org repositories.

Provides an implementation of [Gerrit](https://code.google.com/p/gerrit/) 2.6's CommitValidationListener interface that imposes the following restrictions:

* A project committer can push a commit on behalf of themselves or any other project committer
* A project committer can push a commit on behalf of a contributor if:
 * The contributor has a valid CLA at the time of the push; and
 * The commit message contains a "Signed-off-by:" statement with credentials matching those of the commit author
* A contributor can push a commit if:
 * They have a valid CLA at the time of the push;
 * The commit's author credentials match the user identity;
 * The commit message contains a "Signed-off-by:" statement with credentials matching those of the commit author

An individual is assumed to be a committer if they have PUSH access to the Gerrit project (repository).

An individual is assumed to have a CLA on file if they are a member of the CLA group (currently hardcoded; a future version will make this configurable).

For more information, please see [CLAs in the Ecipse Wiki](http://wiki.eclipse.org/CLA).

Troubleshooting
===============

Push that should otherwise be accepted is being rejected:
* They may be pushing more than one commit. Compare the commit id from the error message with that of the commit; do they match?  

Individual is a committer, but is being rejected:
* Is the email address they're committing with the same as the email address in LDAP?
* Does the corresponding project group have push access on the Gerrit repository?

Individual is not a committer, but has a CLA and is being reject:
* Is the email address they're committing with the same as the email address in LDAP?
* Is the individual in the "Has CLA" LDAP group?
* Is the CLA associated with the right user id?
