# GitHub Secrets Required for Workflows

Your workflows need specific secrets to be configured in GitHub. Here's the complete list and how to set them up.

---

## Overview: All Required Secrets

| Secret Name | Workflow | Purpose | Type |
|------------|----------|---------|------|
| `GOOGLE_SERVICES_TEXT` | build_push.yml | Firebase config | JSON file content |
| `CLIENT_SECRETS_TEXT` | build_push.yml | Google Drive API config | JSON file content |
| `SIGNING_KEY` | build_push.yml | APK signing key | Base64 encoded |
| `ALIAS` | build_push.yml | Signing key alias | Text |
| `KEY_STORE_PASSWORD` | build_push.yml | Keystore password | Text |
| `KEY_PASSWORD` | build_push.yml | Key password | Text |
| `ACCESS_TOKEN` | build_push_preview.yml | GitHub personal access token | Text |

---

## Detailed Breakdown

### 1. GOOGLE_SERVICES_TEXT (Firebase)

**What it is:** The `google-services.json` file from Firebase console

**Where to get it:**
1. Go to: https://console.firebase.google.com/
2. Select your TachiyomiSY project
3. Project settings → Download `google-services.json`

**How to add to GitHub:**
1. Go to: **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `GOOGLE_SERVICES_TEXT`
4. Value: **Paste the entire contents of google-services.json**
   ```json
   {
     "type": "service_account",
     "project_id": "...",
     ...
   }
   ```
5. Click **Add secret**

**Used by:** `build_push.yml` for Firebase initialization

---

### 2. CLIENT_SECRETS_TEXT (Google Drive API)

**What it is:** The client secrets JSON from Google Cloud Console (for Drive API access in your Android app)

**Step-by-step guide to create OAuth client ID:**

#### Step 1: Go to Google Cloud Console
1. Visit: https://console.cloud.google.com/
2. Select your project (or create one if you haven't)
3. Go to: **APIs & Services → Credentials**

#### Step 2: Enable Google Drive API
1. Click **+ ENABLE APIS AND SERVICES** at the top
2. Search for "Google Drive API"
3. Click **Enable**

#### Step 3: Create OAuth Client ID
1. Go back to **APIs & Services → Credentials**
2. Click **+ CREATE CREDENTIALS** button at the top
3. Select **OAuth client ID**

#### Step 4: Configure OAuth Consent Screen (if prompted)
1. If you see "To create an OAuth client ID, you must first configure your consent screen":
   - Click **CONFIGURE CONSENT SCREEN**
   - Select **External** (or Internal if you have a Google Workspace)
   - Fill in required fields:
     - App name: `TachiyomiSY`
     - User support email: your email
     - Developer contact: your email
   - Click **Save and Continue**
   - Skip Scopes (click **Save and Continue**)
   - Add test users if needed (your email)
   - Click **Save and Continue**

#### Step 5: Create Android OAuth Client
1. Back in **Create OAuth client ID** page:
2. **Application type**: Select **Android** (NOT Web application)
3. **Name**: Enter `TachiyomiSY Android` (or any name you prefer)
4. **Package name**: Enter exactly this:
   ```
   eu.kanade.tachiyomi.sy
   ```
5. **SHA-1 certificate fingerprint**: Get it using this command:
   ```bash
   # Use the keystore file you created for signing
   keytool -list -v -keystore /path/to/your/release.jks -alias release -storepass YOUR_KEYSTORE_PASSWORD | grep "SHA1:"

   # Example output:
   # SHA1: A1:B2:C3:D4:E5:F6:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12
   ```
   Copy the SHA1 fingerprint and paste it (with or without colons, both work)

6. Click **CREATE**

#### Step 6: Download the Credentials
`823672767330-gqbp1uc850q146ktk340qi4tnmmkh7hh.apps.googleusercontent.com`
After creating the OAuth client:
1. You'll see your new Android client in the credentials list
2. Click on the **Android client name** you just created
3. Look for **Download JSON** or you can manually copy the client ID and client secret
4. **IMPORTANT**: The JSON structure for Android OAuth is different. You need to create it manually:
   ```json
   {
     "installed": {
       "client_id": "YOUR_CLIENT_ID_HERE.apps.googleusercontent.com",
       "project_id": "your-project-id",
       "auth_uri": "https://accounts.google.com/o/oauth2/auth",
       "token_uri": "https://oauth2.googleapis.com/token",
       "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs"
     }
   }
   ```
   Replace `YOUR_CLIENT_ID_HERE` with the actual client ID shown in the console.

**Alternative: Use Service Account (Recommended for automated workflows)**
If you want automated Drive access without user login:
1. **APIs & Services → Credentials**
2. Click **+ CREATE CREDENTIALS**
3. Select **Service account**
4. Fill in details and click **Create**
5. Click on the created service account
6. Go to **Keys** tab
7. Click **Add Key → Create new key**
8. Select **JSON** format
9. Click **Create** - this downloads the JSON file
10. Use this JSON file content for `CLIENT_SECRETS_TEXT`

**How to add to GitHub:**
1. Go to: **GitHub Repository → Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `CLIENT_SECRETS_TEXT`
4. Value: **Paste the entire contents of the JSON file** (either OAuth or Service Account JSON)
5. Click **Add secret**

**Used by:** `build_push.yml` for Google Drive backup functionality

---

### 3. SIGNING_KEY (APK Signing)

**What it is:** Base64-encoded Android signing key (keystore file)

**How to generate/get it:**

If you already have a keystore:
```bash
# Convert keystore to base64
base64 -i /path/to/your/keystore.jks -o keystore-base64.txt

# Or on macOS:
base64 < /path/to/your/keystore.jks > keystore-base64.txt
```

If you need to create a keystore:
```bash
# Create new signing keystore
keytool -genkey -v -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release \
  -storepass YOUR_KEYSTORE_PASSWORD \
  -keypass YOUR_KEYSTORE_PASSWORD

# Convert to base64
base64 < release.jks > signing_key_base64.txt
```

**How to add to GitHub:**
1. **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `SIGNING_KEY`
4. Value: **Paste the base64 content (the long string)**
5. Click **Add secret**

**Used by:** `build_push.yml` to sign APK variants

---

### 4. ALIAS (Signing Key Alias)

**What it is:** The alias name you gave your signing key in the keystore

**Value:** Usually `release` (unless you named it differently)

**How to check your alias:**
```bash
keytool -list -v -keystore release.jks -storepass YOUR_STORE_PASSWORD
# Look for "Alias name: release"
```

**How to add to GitHub:**
1. **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `ALIAS`
4. Value: `release` (or whatever your alias is)
5. Click **Add secret**

**Example values:** `release`, `tachiyomi-sy`, `prod`

---

### 5. KEY_STORE_PASSWORD (Keystore Password)

**What it is:** The password you set when creating your keystore

**How to add to GitHub:**
1. **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `KEY_STORE_PASSWORD`
4. Value: **Your keystore password**
5. Click **Add secret**

`<YOUR_PASSWORD>`

---

### 6. KEY_PASSWORD (Key Password)

**What it is:** The password for the individual key inside the keystore (usually same as keystore password)

**How to add to GitHub:**
1. **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `KEY_PASSWORD`
4. Value: **Your key password**
5. Click **Add secret**

`<YOUR_PASSWORD>`

---

### 7. ACCESS_TOKEN (GitHub Personal Access Token)
`<YOUR_GITHUB_PAT>`

**What it is:** Personal access token to authenticate with GitHub API for preview builds

**How to create:**
1. Go to: **GitHub Settings → Developer settings → Personal access tokens**
2. Click **Generate new token (classic)**
3. Name: `TachiyomiSY-Preview-Token`
4. Expiration: 90 days or Custom
5. Select scopes:
   - ✅ `repo` (Full control of private repositories)
   - ✅ `workflow` (Update GitHub Action workflows)
6. Click **Generate token**
7. **Copy the token immediately** (you won't see it again)

**How to add to GitHub:**
1. **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Name: `ACCESS_TOKEN`
4. Value: **Paste the token**
5. Click **Add secret**

**Used by:** `build_push_preview.yml` to trigger preview builds in another repo

---

## Step-by-Step Setup Summary

### Step 1: Prepare All Values

| Secret | Source | Format |
|--------|--------|--------|
| GOOGLE_SERVICES_TEXT | Firebase console | JSON content |
| CLIENT_SECRETS_TEXT | Google Cloud | JSON content |
| SIGNING_KEY | Your keystore | Base64 string |
| ALIAS | Your keystore name | Text (e.g., "release") |
| KEY_STORE_PASSWORD | You set this | Text |
| KEY_PASSWORD | You set this | Text |
| ACCESS_TOKEN | GitHub tokens | Text token |

### Step 2: Add to GitHub

1. Go to: **your repo → Settings → Secrets and variables → Actions**
2. For each secret:
   - Click **New repository secret**
   - Enter name and value
   - Click **Add secret**
3. Repeat for all 7 secrets

### Step 3: Verify

Go to **Settings → Secrets and variables → Actions** and verify you see:

```
✅ ACCESS_TOKEN
✅ ALIAS
✅ CLIENT_SECRETS_TEXT
✅ GOOGLE_SERVICES_TEXT
✅ KEY_PASSWORD
✅ KEY_STORE_PASSWORD
✅ SIGNING_KEY
```

All secrets should show with a ● (masked) value - this is correct.

---

## Which Workflows Use Which Secrets?

### build_push.yml (Release Builder)
- ✅ GOOGLE_SERVICES_TEXT
- ✅ CLIENT_SECRETS_TEXT
- ✅ SIGNING_KEY
- ✅ ALIAS
- ✅ KEY_STORE_PASSWORD
- ✅ KEY_PASSWORD

### build_push_preview.yml (Preview Builder)
- ✅ ACCESS_TOKEN

### build_check.yml (PR Build Check)
- ❌ No secrets needed

### test.yml (Unit Tests)
- ❌ No secrets needed

---

## Troubleshooting

### Error: "Failed to retrieve signing key"
- ❌ SIGNING_KEY is not set or incorrect
- ✅ Verify base64 encoding: `base64 < keystore.jks`
- ✅ Verify secret is added in GitHub

### Error: "Invalid keystore"
- ❌ SIGNING_KEY might not be valid base64
- ✅ Check with: `echo $SIGNING_KEY | base64 -d | file -`
- ✅ Should output: "Zip archive data"

### Error: "Bad password"
- ❌ KEY_STORE_PASSWORD or KEY_PASSWORD is incorrect
- ✅ Verify password matches keystore

### Error: "Alias not found"
- ❌ ALIAS doesn't match an entry in keystore
- ✅ Check with: `keytool -list -v -keystore keystore.jks`

### Error: "Invalid GitHub token"
- ❌ ACCESS_TOKEN is expired or invalid
- ✅ Generate new token in GitHub settings
- ✅ Make sure scopes include `repo` and `workflow`

---

## Security Best Practices

✅ **DO:**
- Store secrets in GitHub Secrets (encrypted)
- Never commit secrets to git
- Use strong passwords for keystore
- Rotate tokens periodically
- Set token expiration dates

❌ **DON'T:**
- Put secrets in code files
- Commit keystore files
- Share tokens via email/chat
- Use same passwords everywhere
- Leave old tokens active

---

## Rotating Secrets

### Rotate API Keys/Tokens

1. Generate new token (see ACCESS_TOKEN section)
2. Update secret in GitHub
3. Delete old token from GitHub settings
4. Verify new token works with test workflow

### Rotate Signing Key

1. Generate new keystore (keytool command above)
2. Update SIGNING_KEY secret (new base64)
3. Update ALIAS if changed
4. Update passwords if changed
5. Test with build_push.yml

---

## Related Documentation

- `docs/index.md` - Main developer guide
- `docs/WORKFLOW-AND-BRANCH-PROTECTION.md` - Workflow details
- `docs/SETUP-COMPLETE-SUMMARY.md` - Overall setup

---

**Status: 🟢 Ready to add all secrets to GitHub**

**Next:** Add all 7 secrets following the instructions above, then test with a release!
