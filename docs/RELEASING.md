# GitHub release protocol

GitHub Releases and Pages are the only distribution targets. Public repositories,
standard Actions runners and Pages are free. Never publish an unsigned APK or
push while checks fail. Keep published commits and tags immutable.

## One-time owner signing setup

The owner authorized local key creation and GitHub secret setup on 8 November 2026;
the first release key is configured. Preserve the ignored `.tools/signing/` folder.
Its password record is protected with Windows DPAPI for this Windows account.
An encrypted owner-controlled backup is still needed before moving machines:
DPAPI alone does not provide portable recovery. Never regenerate the update key.

For a new independent fork, follow the setup below. Never send signing files, passwords or tokens
in chat, commit them, paste them into a command line, or include them in logs.

1. In Android Studio use **Build → Generate Signed Bundle / APK → APK** to create
   a private release key. Save it inside the ignored project folder
   `D:/FreeCodeChats/ScreenGarden/.tools/signing/`. Use a long-lived certificate.
   Keep an encrypted owner-controlled backup: losing the key prevents normal updates.
2. In [repository Actions secrets](https://github.com/34rns/ScreenGarden/settings/secrets/actions)
   configure `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`. The first is the base64 of the
   signing file, entered privately; use the helper below to upload it without printing.
   Set the remaining three directly in GitHub's private secret forms.
3. To upload the file without displaying it, run from the repository root:

   ```powershell
   ./scripts/Upload-SigningFile.ps1 -Path .tools/signing/your-release-key.jks
   ```

The helper accepts only a project-contained `.jks`/`.keystore` file, passes encoded
bytes to `gh secret set` through standard input and never writes an encoded copy.
It does not modify the signing file. GitHub secret names may be inspected; values
must never be retrieved or displayed. No new token or paid account is required.

Local signed builds use the four `SG_RELEASE_*` environment variables documented
in `app/build.gradle.kts`; use a private environment, never command-line passwords.
Without those variables `assembleRelease` is an internal unsigned R8 check only.
The public workflow refuses missing signing secrets and verifies the signature.

## Every release

1. Increment `versionCode` and set `versionName` in `version.properties` for an
   update. Prepare matching `docs/releases/vVERSION.md` with features, changes,
   migration information, verified results and practical limitations.
2. Run `gradlew.bat ktlintCheck --no-build-cache`, `:app:testDebugUnitTest`,
   `:app:lintDebug`, `:app:assembleDebug`, and the repository/privacy/secret checks.
   Run applicable emulator integration and migration tests. A schema version
   requires a new migration, preservation test and exported immutable schema.
3. Refresh affected screenshots on the dedicated emulator with fake debug-only data;
   inspect the actual screen and update the gallery and corresponding `site/screenshots/`
   copies. CI checks that these copies exactly match their originals. Never capture real usage.
4. Update `AGENT_MEMORY.txt`, commit with Conventional Commits, push `main` and wait
   for green CI. Run **Signed GitHub release** manually from `main` as a preflight:
   it signs and verifies artifacts but cannot publish. Wait for that check to pass
   before creating the matching new tag from the verified commit:

   ```powershell
   git tag v0.1.0
   git push origin v0.1.0
   ```

   Replace the example for future versions. Never overwrite an existing tag.
5. Watch **Signed GitHub release**. Its tag must match version.properties. It reruns
   all four gates, production lint, repository checks, signing and alignment before
   uploading public artifacts. A dependent job verifies checksums and publishes a
   GitHub Release. A publishing retry must select the existing release tag.
6. Install the downloaded signed APK on the dedicated emulator, verify onboarding,
   optional-permission behavior, focus start/stop and no crash. Check its signing
   certificate matches the previous release for updates. Do not photograph real data.
7. Confirm the Pages site and APK download work; inspect desktop/mobile browser
   layouts and keyboard navigation. Save final CI, release, site and verification
   evidence in the memory. Report Verified versus Not verified accurately.

## Pages and reproducibility

The Pages workflow deploys static `site/` files and the committed emulator images.
Enable **GitHub Actions** as the Pages source once. No tracking, web font/CDN,
external script, build package or API call is required by the site.

Versions and action commits are pinned; the Gradle distribution SHA-256 is checked.
No proprietary blob or paid build service ships. This is reproducible-friendly;
byte-for-byte independent reproduction has not been verified. Signing is intentionally
private; corresponding source is available at each immutable release tag.
