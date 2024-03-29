gerrit-eca-plugin
=================

---
**NOTE**

This project was migrated to [Eclipse Gitlab](https://gitlab.eclipse.org/eclipsefdn/it/webdev/gerrit-eca-plugin) on October 14, 2021.

---


A Gerrit plugin for controlling pushes to eclipse.org repositories.

Provides an implementation of [Gerrit](https://code.google.com/p/gerrit/) 3.2's CommitValidationListener interface that imposes the following restrictions:

* A project committer can push a commit on behalf of themselves or any other project committer
* A project committer can push a commit on behalf of a contributor if:
    * The contributor has a valid ECA at the time of the push; and
    * The commit message contains a "Signed-off-by:" statement with credentials matching those of the commit author
* A contributor can push a commit if:
    * They have a valid ECA at the time of the push;
    * The commit's author credentials match the user identity;
    * The commit message contains a "Signed-off-by:" statement with credentials matching those of the commit author

An individual is assumed to be a committer if they have PUSH access to the Gerrit project (repository).

An individual is assumed to have a ECA on file if they are a member of the ECA group (currently hardcoded; a future version will make this configurable).

For more information, please see [ECA in the Ecipse Wiki](http://wiki.eclipse.org/ECA).

Troubleshooting
===============

Push that should otherwise be accepted is being rejected:
* They may be pushing more than one commit. Compare the commit id from the error message with that of the commit; do they match?  

Individual is a committer, but is being rejected:
* Is the email address they're committing with the same as the email address in LDAP?
* Does the corresponding project group have push access on the Gerrit repository?

Individual is not a committer, but has a ECA and is being reject:
* Is the email address they're committing with the same as the email address in LDAP?
* Is the individual in the "Has ECA" LDAP group?
* Is the ECA associated with the right user id?
