# 📱 Step-by-Step Guide: Building APK with GitHub Actions & Releasing

This guide explains how to build APK files using GitHub Actions and automatically attach them to GitHub Releases.

---

## 📋 Prerequisites

Before you can build and release APKs, ensure you have these GitHub Secrets configured:

### Required Secrets

| Secret Name | Description | How to Get It |
|------------|-------------|---------------|
| `GOOGLE_SERVICES_TEXT` | Firebase configuration JSON | Download from Firebase Console |
| `CLIENT_SECRETS_TEXT` | Google Drive API credentials | Download from Google Cloud Console |
| `SIGNING_KEY` | Base64-encoded keystore file | Convert your `.jks` file to base64 |
| `ALIAS` | Keystore alias name | Usually `release` or your alias |
| `KEY_STORE_PASSWORD` | Keystore password | Your keystore password |
| `KEY_PASSWORD` | Key password | Your key password |

**Setup Instructions:** See `docs/GITHUB-SECRETS-REQUIREDs.md` for detailed setup.

---

## 🚀 Step-by-Step Process

### 📍 Starting Point: Which Branch Are You On?

**Decision Tree:**

```
Are you on the develop branch?
│
├─ YES → You have features merged to develop
│        → Use Workflow A: Merge develop → release (Recommended)
│
└─ NO → Are you already on release branch?
         │
         ├─ YES → Use Workflow B: Direct release workflow
         │
         └─ NO → Switch to release branch first
                  git checkout release
                  git pull origin release
                  → Then use Workflow B
```

**Two scenarios:**

1. **Starting from `develop` branch** (Recommended - Proper Workflow)
   - ✅ You've merged features to `develop`
   - ✅ Following proper git workflow
   - ✅ Ensures all features are included
   - → Follow **Workflow A** below

2. **Already on `release` branch**
   - ✅ You're directly working on `release`
   - ✅ Quick workflow for direct releases
   - → Follow **Workflow B** below

**💡 Recommendation:** Use **Workflow A** (from `develop`) as it follows your established git workflow where features flow: `develop` → `release` → `master`.

---

## 🔄 Workflow A: Starting from `develop` Branch (Recommended)

This is the **proper workflow** according to your git branching strategy.

### Step 1: Ensure `develop` is Up to Date

```bash
# Make sure you're on develop branch
git checkout develop
git pull origin develop

# Verify all features are merged and tested
```

### Step 2: Merge `develop` into `release`

```bash
# Switch to release branch
git checkout release
git pull origin release

# Merge develop into release (brings all new features)
git merge develop

# If there are merge conflicts, resolve them:
# ... fix conflicts ...
# git add .
# git commit -m "merge: integrate develop into release"
```

**Why merge first?** This ensures all features from `develop` are included in the release.

### Step 3: Update Version Number

**Location:** `app/build.gradle.kts`

**Understanding Semantic Versioning:**

Version format: `MAJOR.MINOR.PATCH` (e.g., `1.12.1`)
- **MAJOR** (1): Breaking changes, incompatible API changes
- **MINOR** (12): New features, backward compatible
- **PATCH** (1): Bug fixes, backward compatible

**What to update:**
```kotlin
defaultConfig {
    versionCode = 76        // ← Always increment by 1 (required by Android)
    versionName = "1.12.1"  // ← Update based on change type (see below)
}
```

**Version Update Examples:**

#### Patch Version (Bug Fixes)
**When:** Bug fixes, small improvements, no new features
```kotlin
// Current
versionCode = 75
versionName = "1.12.0"

// After patch update
versionCode = 76
versionName = "1.12.1"  // ← Increment PATCH
```

#### Minor Version (New Features)
**When:** New features added, backward compatible
```kotlin
// Current
versionCode = 75
versionName = "1.12.0"

// After minor update
versionCode = 76
versionName = "1.13.0"  // ← Increment MINOR, reset PATCH to 0
```

#### Major Version (Breaking Changes)
**When:** Breaking changes, major redesign, incompatible changes
```kotlin
// Current
versionCode = 75
versionName = "1.12.0"

// After major update
versionCode = 76
versionName = "2.0.0"  // ← Increment MAJOR, reset MINOR and PATCH to 0
```

**Quick Decision Guide:**

| Change Type | Version Update | Example |
|------------|----------------|---------|
| Bug fix, small improvement | **PATCH** (1.12.0 → 1.12.1) | Fixed crash, improved performance |
| New feature, enhancement | **MINOR** (1.12.0 → 1.13.0) | Added dark mode, new download feature |
| Breaking change, major redesign | **MAJOR** (1.12.0 → 2.0.0) | Changed API, removed feature, incompatible changes |

**Still unsure?** Ask yourself:
- Does this break existing functionality? → **MAJOR**
- Does this add new functionality? → **MINOR**
- Does this only fix bugs? → **PATCH**

**Important Notes:**
- **`versionCode`**: Always increment by 1, regardless of version type. Android requires this to be a unique, increasing integer.
- **`versionName`**: Follow semantic versioning rules above.
- **Version Code vs Version Name**: `versionCode` is for Android Play Store (must always increase), `versionName` is for users (semantic versioning).

### Step 4: Commit Version Change

```bash
# Edit app/build.gradle.kts (update version)
# ... make your changes ...

# Commit the version bump (use appropriate message)
git add app/build.gradle.kts

# For patch version
git commit -m "bump: version to 1.12.1"

# For minor version
git commit -m "bump: version to 1.13.0"

# For major version
git commit -m "bump: version to 2.0.0"
```

### Step 5: Push to Release Branch

```bash
# Push to trigger the workflow
git push origin release
```

**What happens next:**
- GitHub Actions detects the push to `release` branch
- Workflow `.github/workflows/build_push.yml` automatically starts
- You can monitor progress at: `https://github.com/YOUR_USERNAME/TachiyomiSY/actions`

---

## 🔄 Workflow B: Already on `release` Branch

If you're already working directly on the `release` branch:

### Step 1: Update Version Number

**Location:** `app/build.gradle.kts`

**Understanding Semantic Versioning:**

Version format: `MAJOR.MINOR.PATCH` (e.g., `1.12.1`)
- **MAJOR** (1): Breaking changes, incompatible API changes
- **MINOR** (12): New features, backward compatible
- **PATCH** (1): Bug fixes, backward compatible

**What to update:**
```kotlin
defaultConfig {
    versionCode = 76        // ← Always increment by 1 (required by Android)
    versionName = "1.12.1"  // ← Update based on change type (see below)
}
```

**Version Update Examples:**

#### Patch Version (Bug Fixes)
**When:** Bug fixes, small improvements, no new features
```kotlin
// Current
versionCode = 75
versionName = "1.12.0"

// After patch update
versionCode = 76
versionName = "1.12.1"  // ← Increment PATCH
```

#### Minor Version (New Features)
**When:** New features added, backward compatible
```kotlin
// Current
versionCode = 75
versionName = "1.12.0"

// After minor update
versionCode = 76
versionName = "1.13.0"  // ← Increment MINOR, reset PATCH to 0
```

#### Major Version (Breaking Changes)
**When:** Breaking changes, major redesign, incompatible changes
```kotlin
// Current
versionCode = 75
versionName = "1.12.0"

// After major update
versionCode = 76
versionName = "2.0.0"  // ← Increment MAJOR, reset MINOR and PATCH to 0
```

**Quick Decision Guide:**

| Change Type | Version Update | Example |
|------------|----------------|---------|
| Bug fix, small improvement | **PATCH** (1.12.0 → 1.12.1) | Fixed crash, improved performance |
| New feature, enhancement | **MINOR** (1.12.0 → 1.13.0) | Added dark mode, new download feature |
| Breaking change, major redesign | **MAJOR** (1.12.0 → 2.0.0) | Changed API, removed feature, incompatible changes |

**Still unsure?** Ask yourself:
- Does this break existing functionality? → **MAJOR**
- Does this add new functionality? → **MINOR**
- Does this only fix bugs? → **PATCH**

**Important Notes:**
- **`versionCode`**: Always increment by 1, regardless of version type. Android requires this to be a unique, increasing integer.
- **`versionName`**: Follow semantic versioning rules above.
- **Version Code vs Version Name**: `versionCode` is for Android Play Store (must always increase), `versionName` is for users (semantic versioning).

### Step 2: Commit Version Change

```bash
# Make sure you're on the release branch
git checkout release
git pull origin release

# Edit app/build.gradle.kts (update version)
# ... make your changes ...

# Commit the version bump (use appropriate message)
git add app/build.gradle.kts

# For patch version
git commit -m "bump: version to 1.12.1"

# For minor version
git commit -m "bump: version to 1.13.0"

# For major version
git commit -m "bump: version to 2.0.0"
```

### Step 3: Push to Release Branch

```bash
# Push to trigger the workflow
git push origin release
```

**What happens next:**
- GitHub Actions detects the push to `release` branch
- Workflow `.github/workflows/build_push.yml` automatically starts
- You can monitor progress at: `https://github.com/YOUR_USERNAME/TachiyomiSY/actions`

---

## ⚙️ What Happens in GitHub Actions (Automated)

The workflow (`.github/workflows/build_push.yml`) performs these steps automatically:

### Phase 1: Environment Setup
1. **Clone Repository** - Checks out your code
2. **Setup Android SDK** - Installs Android build tools
3. **Setup JDK 17** - Configures Java environment
4. **Setup Gradle** - Prepares Gradle build system

### Phase 2: Configuration Files
5. **Write google-services.json** - Creates Firebase config from secret
6. **Write client_secrets.json** - Creates Google Drive API config from secret

### Phase 3: Code Quality Checks
7. **Check Code Format** - Runs `./gradlew spotlessCheck` (ensures code style)
8. **Build App** - Runs `./gradlew assembleStandardRelease` (compiles APK)

### Phase 4: Testing
9. **Run Unit Tests** - Executes `testReleaseUnitTest` and `testStandardReleaseUnitTest`

### Phase 5: Signing
10. **Sign APK** - Signs all APK variants using your signing key from secrets
    - Uses `r0adkll/sign-android-release@v1` action
    - Signs: universal, arm64-v8a, armeabi-v7a, x86, x86_64

### Phase 6: Prepare Release Files
11. **Clean up build artifacts** - Renames signed APKs:
    - `TachiyomiSY.apk` (universal - recommended)
    - `TachiyomiSY-arm64-v8a.apk`
    - `TachiyomiSY-armeabi-v7a.apk`
    - `TachiyomiSY-x86.apk`
    - `TachiyomiSY-x86_64.apk`

### Phase 7: Create GitHub Release
12. **Create Release** - Uses `softprops/action-gh-release@v2`:
    - **Tag:** `github.run_number` (e.g., "123")
    - **Name:** "TachiyomiSY"
    - **Body:** Includes installation tip
    - **Files:** All 5 APK variants attached
    - **Status:** `draft: true` (needs manual publishing)

---

## ✅ Step 4: Verify Build Success

### Check GitHub Actions

1. Go to: `https://github.com/YOUR_USERNAME/TachiyomiSY/actions`
2. Find the latest "Release Builder" workflow run
3. Check that all steps show ✅ (green checkmarks)

**If any step fails:**
- Click on the failed step to see error logs
- Fix the issue and push again

### Check Release Page

1. Go to: `https://github.com/YOUR_USERNAME/TachiyomiSY/releases`
2. You should see a new **Draft** release
3. The release should have:
   - Tag: `github.run_number` (e.g., "123")
   - 5 APK files attached
   - Status: **Draft** (not published yet)

---

## 📤 Step 5: Publish the Release

### Option A: GitHub Web UI (Recommended)

1. Go to: `https://github.com/YOUR_USERNAME/TachiyomiSY/releases`
2. Find the draft release
3. Click **Edit** (pencil icon)
4. (Optional) Update release title and description
5. Click **Publish release** button

**The release is now live!** Users can download APKs from the Releases page.

### Option B: GitHub CLI

```bash
# List draft releases
gh release list --limit 5

# Publish a specific draft release
gh release edit <TAG_NAME> --draft=false

# Example:
gh release edit 123 --draft=false
```

---

## 🔄 Step 6: Sync Master Branch (Optional)

After publishing the release, sync your `master` branch:

```bash
# Switch to master
git checkout master
git pull origin master

# Merge release branch
git merge release

# Push to master
git push origin master
```

**Why:** Keeps `master` in sync with released versions.

---

## 📦 APK Variants Explained

Your workflow builds 5 APK variants:

| APK File | Architecture | Size | Recommended For |
|---------|--------------|------|------------------|
| `TachiyomiSY.apk` | **Universal** | Largest | **Most users** (recommended) |
| `TachiyomiSY-arm64-v8a.apk` | ARM 64-bit | Smaller | Modern Android devices |
| `TachiyomiSY-armeabi-v7a.apk` | ARM 32-bit | Smaller | Older Android devices |
| `TachiyomiSY-x86.apk` | x86 32-bit | Smaller | Emulators, Intel devices |
| `TachiyomiSY-x86_64.apk` | x86 64-bit | Smaller | x86_64 emulators |

**💡 Tip:** If unsure, users should download `TachiyomiSY.apk` (universal).

---

## 🔍 Troubleshooting

### Build Fails at "Check code format"

**Problem:** Code style violations detected by Spotless

**Solution:**
```bash
# Fix formatting locally
./gradlew spotlessApply

# Commit and push again
git add .
git commit -m "chore: fix code formatting"
git push origin release
```

---

### Build Fails at "Sign APK"

**Problem:** Signing secrets are incorrect or missing

**Solution:**
1. Check GitHub Secrets are set correctly:
   - `SIGNING_KEY` (base64-encoded keystore)
   - `ALIAS`
   - `KEY_STORE_PASSWORD`
   - `KEY_PASSWORD`

2. Verify keystore is valid:
```bash
# Test keystore locally
keytool -list -v -keystore release.jks -alias release
```

3. Re-encode keystore if needed:
```bash
base64 < release.jks > keystore-base64.txt
# Copy contents to SIGNING_KEY secret
```

---

### Tests Fail

**Problem:** Unit tests are failing

**Solution:**
1. Run tests locally:
```bash
./gradlew testReleaseUnitTest testStandardReleaseUnitTest
```

2. Fix failing tests
3. Commit and push again

---

### Release Created but No APKs Attached

**Problem:** APK files not found or path incorrect

**Solution:**
1. Check workflow logs for "Create release" step
2. Verify APK files exist in expected location:
   - `app/build/outputs/apk/standard/release/`
3. Check file renaming step completed successfully

---

### Workflow Doesn't Trigger

**Problem:** Push to `release` branch doesn't start workflow

**Solution:**
1. Verify you pushed to `release` branch (not `master` or `develop`)
2. Check workflow file exists: `.github/workflows/build_push.yml`
3. Verify workflow syntax is valid (check Actions tab for errors)
4. Check branch name matches exactly: `release` (case-sensitive)

---

## 📊 Workflow Summary

### Workflow A: From `develop` Branch

```
┌─────────────────────────────────────────────────────────┐
│ 1. Merge develop into release                           │
│    git checkout develop                                  │
│    git pull origin develop                               │
│    git checkout release                                  │
│    git merge develop                                     │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Update version in app/build.gradle.kts               │
│    versionCode = 76 (always +1)                          │
│    versionName = "1.12.1" (patch)                        │
│    OR "1.13.0" (minor) OR "2.0.0" (major)                │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Commit and push to release branch                    │
│    git commit -m "bump: version to 1.12.1"              │
│    git push origin release                               │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 4. GitHub Actions Workflow Runs (Automatic)             │
│    ✅ Setup environment                                  │
│    ✅ Write config files                                 │
│    ✅ Check code format                                  │
│    ✅ Build APK                                          │
│    ✅ Run tests                                          │
│    ✅ Sign APKs                                          │
│    ✅ Create draft release with APKs                     │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Verify Build Success                                  │
│    Check GitHub Actions: ✅ All green                    │
│    Check Releases: Draft release with APKs               │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Publish Release (Manual)                             │
│    GitHub UI: Click "Publish release"                    │
│    OR                                                     │
│    CLI: gh release edit <TAG> --draft=false              │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 7. Release is Live! 🚀                                  │
│    Users can download APKs from Releases page           │
└─────────────────────────────────────────────────────────┘
```

### Workflow B: Directly on `release` Branch

```
┌─────────────────────────────────────────────────────────┐
│ 1. Update version in app/build.gradle.kts               │
│    versionCode = 76 (always +1)                          │
│    versionName = "1.12.1" (patch)                        │
│    OR "1.13.0" (minor) OR "2.0.0" (major)                │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Commit and push to release branch                    │
│    git commit -m "bump: version to 1.12.1"              │
│    git push origin release                               │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 3. GitHub Actions Workflow Runs (Automatic)             │
│    ✅ Setup environment                                  │
│    ✅ Write config files                                 │
│    ✅ Check code format                                  │
│    ✅ Build APK                                          │
│    ✅ Run tests                                          │
│    ✅ Sign APKs                                          │
│    ✅ Create draft release with APKs                     │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Verify Build Success                                  │
│    Check GitHub Actions: ✅ All green                    │
│    Check Releases: Draft release with APKs               │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Publish Release (Manual)                             │
│    GitHub UI: Click "Publish release"                    │
│    OR                                                     │
│    CLI: gh release edit <TAG> --draft=false              │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Release is Live! 🚀                                  │
│    Users can download APKs from Releases page           │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Quick Reference Commands

### Workflow A: From `develop` Branch (Recommended)

```bash
# 1. Start from develop
git checkout develop
git pull origin develop

# 2. Merge develop into release
git checkout release
git pull origin release
git merge develop

# 3. Update version in app/build.gradle.kts
#    Choose version type:
#    - Patch: 1.12.0 → 1.12.1 (bug fixes)
#    - Minor: 1.12.0 → 1.13.0 (new features)
#    - Major: 1.12.0 → 2.0.0 (breaking changes)
#    Always increment versionCode by 1

# 4. Commit and push
git add app/build.gradle.kts
git commit -m "bump: version to 1.12.1"  # or 1.13.0 or 2.0.0
git push origin release

# 5. Wait for GitHub Actions to complete
# Then publish draft release via GitHub UI or CLI

# 6. Sync master (optional)
git checkout master
git pull origin master
git merge release
git push origin master
```

### Workflow B: Directly on `release` Branch

```bash
# 1. Switch to release
git checkout release
git pull origin release

# 2. Update version in app/build.gradle.kts
#    Choose version type:
#    - Patch: 1.12.0 → 1.12.1 (bug fixes)
#    - Minor: 1.12.0 → 1.13.0 (new features)
#    - Major: 1.12.0 → 2.0.0 (breaking changes)
#    Always increment versionCode by 1

# 3. Commit and push
git add app/build.gradle.kts
git commit -m "bump: version to 1.12.1"  # or 1.13.0 or 2.0.0
git push origin release

# 4. Wait for GitHub Actions to complete
# Then publish draft release via GitHub UI or CLI

# 5. Sync master (optional)
git checkout master
git pull origin master
git merge release
git push origin master
```

---

## 📝 Notes

- **Draft Releases:** The workflow creates **draft** releases. You must manually publish them.
- **Tag Format:** Currently uses `github.run_number` (e.g., "123") instead of semantic version (e.g., "v1.12.1")
- **APK Location:** All APKs are automatically attached to the release
- **Build Time:** Typically takes 5-15 minutes depending on GitHub Actions queue
- **Secrets:** Never commit secrets to the repository. Always use GitHub Secrets.

---

## 🔗 Related Documentation

- **Secrets Setup:** `docs/GITHUB-SECRETS-REQUIREDs.md`
- **Workflow Details:** `docs/SETUP-COMPLETE-SUMMARYc.md`
- **Git Workflow:** `docs/index.md` (if exists)

---

**Questions?** Check the workflow logs in GitHub Actions or review `.github/workflows/build_push.yml` for details.
