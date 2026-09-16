# Security Policy

## Intentional Credential Exposure
Revenger uses an automated, batch-generation packaging infrastructure which deliberately exposes the following keystore information:
- Release build password: `ludere`
- Alias: `key0`
- The `revenger.jks` file bundled with the repository.

**This is not a vulnerability.** These credentials are required to remain public-domain so users can synthesize their own functional LibRetro ROM bundles without maintaining their own signing infrastructure.

Please do not open issues regarding local keystore or `app/build.gradle` credential exposure.
