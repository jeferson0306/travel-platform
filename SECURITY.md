# Security Policy

## Reporting a vulnerability

This is a personal, non-commercial engineering showcase project. If you find a
security issue, please open a private report via GitHub's
["Report a vulnerability"](../../security/advisories/new) feature instead of a
public issue. Do not include exploit details in a public thread.

## What never goes into this repository

Because this repository is public, the following are treated as hard
violations and will be scrubbed from history if they ever land:

- Real credentials, API keys, tokens, or passwords of any kind
- Real personal data (names, emails, phone numbers, documents, addresses)
  belonging to the maintainer or anyone else
- Production endpoints, internal hostnames, or infrastructure details that are
  not meant to be public
- Customer or user data of any kind (all seed/sample data is synthetic)

## How this is enforced

- `.gitignore` excludes `.env`, key/cert files, Terraform state, and anything
  under `secrets/`.
- A pre-commit hook runs [gitleaks](https://github.com/gitleaks/gitleaks)
  against every staged change (`infrastructure/scripts/install-git-hooks.sh`).
- CI runs a secret scan and dependency/container vulnerability scans
  (Trivy, OWASP Dependency-Check) on every pull request.
- LocalStack and all local infrastructure use well-known fake credentials
  (`test` / `test`) that are explicitly documented as non-secret in
  [.env.example](.env.example).
- All Docker images are pinned to a specific version tag from an official,
  verified publisher — never `latest`, never an unverified third-party image.

## Supported versions

This project does not follow a formal LTS/support-window model; security
fixes are applied to `main` and the latest release.
