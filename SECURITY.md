# Security policy

Security fixes target the latest published release and `main`. Older versions
should be updated before investigating a report. This is a volunteer project;
there is no guaranteed response deadline.

## Report privately

Use [GitHub's private vulnerability reporting](https://github.com/34rns/ScreenGarden/security/advisories/new)
when available. If unavailable, ask the repository owner for a private contact route
without disclosing the vulnerability publicly. Include the version, Android version,
reproduction steps and practical impact using artificial data.

Never attach passwords, signing material, tokens, `local.properties`, personal
screen-time screenshots or an unredacted backup. A JSON backup may reveal usage
history even though the app has no internet permission.

## Scope and boundaries

Relevant reports include unsafe backup parsing, unintended exports, exposed app
components, unauthorized local state changes and regressions in the offline manifest.
Missing OEM usage events and delayed work are documented platform limits. Optional
focus blocking is a voluntary aid, not parental control or a security boundary.

The app stores data locally without an additional encryption layer. Device security
and your chosen document provider govern access to exports. Automatic backup is
disabled and no network client is included. Those claims are checked in CI.
