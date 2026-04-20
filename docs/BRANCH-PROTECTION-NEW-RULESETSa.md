# GitHub Rulesets: New Branch Protection Interface

GitHub has moved to a new **Rulesets** system (replacing the old "Branch protection rules"). This is more powerful and flexible.

## Overview of New Rulesets

The new interface is at: **Settings → Rules → Rulesets**

It consists of:
- **Ruleset Name** - Name for this set of rules (e.g., "master", "develop", "release")
- **Enforcement status** - Active/Disabled
- **Target branches** - Which branches this applies to
- **Bypass list** - Who can bypass these rules
- **Rules** - What rules to enforce (PR reviews, status checks, etc.)

---

## Create Ruleset for `master` Branch

### Step 1: Start New Ruleset

1. Go to: **Settings → Rules → Rulesets**
2. Click **New branch ruleset**

### Step 2: Configure Ruleset Name and Target

**Ruleset Name:**
```
master-protection
```

**Enforcement status:**
- Select: **Active** (dropdown)

**Target branches:**
- Click **Add target**
- Select: **Branch name pattern**
- Enter: `master`
- Click **Add**

**Bypass list:** (Leave empty for now, or add yourself if you want to be able to bypass)
- Leave as "Bypass list is empty"

### Step 3: Add Rules

Scroll down to **Rules** section and enable these:

#### Rule 1: Require a pull request before merging
- ✅ Check the box: **Require a pull request before merging**
- Required number of approvals: `1`
- ✅ Check: **Dismiss stale pull request approvals when new commits are pushed**
- ❌ Uncheck: **Require review from Code Owners**
- ❌ Uncheck: **Require approval of the most recent reviewable push**

#### Rule 2: Require conversation resolution
- ✅ Check: **Require all conversations on code to be resolved before merging**

#### Rule 3: Prevent force pushes
- ✅ Check: **Prevent force pushes**

#### Rule 4: Prevent deletions
- ✅ Check: **Prevent deletion of matching branches**

### Step 4: Save

- Click **Create** button

---

## Create Ruleset for `develop` Branch

### Step 1: Start New Ruleset

1. Click **New branch ruleset**

### Step 2: Configure

**Ruleset Name:**
```
develop-protection
```

**Enforcement status:** Active

**Target branches:**
- Add target: **Branch name pattern** = `develop`

**Bypass list:** Empty

### Step 3: Add Rules

#### Rule 1: Require a pull request before merging
- ✅ Enable
- Required approvals: `1`
- ✅ Dismiss stale reviews
- ❌ Code owners required

#### Rule 2: Require status checks
- ✅ Check: **Require status checks to pass**
- ✅ Check: **Require branches to be up to date before merging**
- Required status checks:
  - Click **Add checks** or **Add a required status check**
  - Add: `build_check`
  - Add: `test`

#### Rule 3: Require conversation resolution
- ✅ Check: **Require all conversations on code to be resolved before merging**

#### Rule 4: Prevent force pushes
- ✅ Check: **Prevent force pushes**

#### Rule 5: Prevent deletions
- ✅ Check: **Prevent deletion of matching branches**

### Step 4: Save

- Click **Create** button

---

## Create Ruleset for `release` Branch

### Step 1: Start New Ruleset

1. Click **New branch ruleset**

### Step 2: Configure

**Ruleset Name:**
```
release-protection
```

**Enforcement status:** Active

**Target branches:**
- Add target: **Branch name pattern** = `release`

**Bypass list:** Empty (or add yourself if desired)

### Step 3: Add Rules

#### Rule 1: Prevent force pushes
- ✅ Check: **Prevent force pushes**

#### Rule 2: Prevent deletions
- ✅ Check: **Prevent deletion of matching branches**

#### Rule 3: (Optional) Require conversation resolution
- ✅ Check: **Require all conversations on code to be resolved before merging**

**Note:** No PR requirement for release - allows direct push to trigger `build_push.yml`

If you prefer PR workflow for release, add:
- ✅ Require a pull request before merging (1 approval)
- ✅ Dismiss stale reviews
- ✅ Require conversation resolution

### Step 4: Save

- Click **Create** button

---

## Verify Rulesets Were Created

After creating all 3 rulesets, you should see:

**Settings → Rules → Rulesets**

```
Rulesets
✅ master-protection (Active)
   └─ Target: master
   └─ Rules: PR required, conversation resolution, no force push, no deletion

✅ develop-protection (Active)
   └─ Target: develop
   └─ Rules: PR required, status checks (build_check, test), up to date, conversation resolution, no force push, no deletion

✅ release-protection (Active)
   └─ Target: release
   └─ Rules: No force push, no deletion
```

---

## How Rulesets Work

When someone pushes/merges:

1. **Check target branches**
   - Does this push match the target pattern?
   - (e.g., pushing to `develop` matches `develop` target)

2. **Apply rules**
   - If matches, apply all rules in that ruleset
   - Rules are enforced simultaneously

3. **Allow/Block action**
   - If all rules pass: ✅ allow
   - If any rule fails: ❌ block with error message

---

## Differences from Old System

| Old (Branch Protection) | New (Rulesets) |
|-----------|-----------|
| "Branch protection rules" | "Rulesets" |
| One rule = one branch | One ruleset = multiple branches/patterns |
| Limited rule options | Many more rule options |
| Rules for one branch | Can apply one ruleset to multiple branches |
| Harder to manage multiple rules | Easier to organize related rules |

---

## Test Your Rulesets

### Test `develop` Ruleset

1. Create a test branch:
```bash
git checkout develop
git pull origin develop
git checkout -b test/rulesets
echo "test" > test.txt
git add test.txt
git commit -m "test: verify rulesets"
git push -u origin test/rulesets
```

2. Create PR on GitHub (target: `develop`)

3. Verify:
   - ✅ Must see "Checks" section
   - ✅ `build_check` must run
   - ✅ `test` must run
   - ✅ Cannot merge until both pass and approved

4. Merge (if checks pass):
   - Cannot merge without approval (blocked by ruleset)
   - Approve PR
   - Squash and merge

---

### Test `master` Ruleset

1. Create PR from `develop` → `master`

2. Verify:
   - ✅ Cannot merge without approval (blocked by ruleset)
   - ✅ Cannot bypass ruleset (unless you're in bypass list)
   - ✅ Requires conversation resolution

3. Approve and merge

---

## Common Ruleset Rules

Here are the most useful rules available:

- **Require a pull request before merging**
  - Required number of approvals
  - Dismiss stale reviews
  - Code owner reviews required
  - Require status checks to pass
  - Require branches to be up to date
  - Require all conversations resolved

- **Restrict push and force push events**
  - Prevent push to matching branches
  - Prevent force pushes

- **Restrict deletion of matching branches**
  - Prevent branch deletion

- **Restrict creations of matching refs**
  - Prevent creation of branches matching pattern

- **Require code scanning to pass**
  - Enforce security scanning

- **Require deployments to succeed**
  - Enforce deployment checks

---

## FAQ

### Q: Can I bypass these rules?
**A:** Yes, add yourself to the "Bypass list" if needed. Or GitHub admins can always bypass.

### Q: Can I apply the same rules to multiple branches?
**A:** Use glob patterns in target branches:
- `master` - exact match
- `main` - exact match
- `release/*` - all branches starting with "release/"
- `v*.*.* ` - version tags

### Q: What if a check fails?
**A:** The PR will show as blocked. Fix the issue and push a new commit. CI re-runs automatically.

### Q: Can I edit a ruleset after creating it?
**A:** Yes! Click the pencil icon next to the ruleset name to edit.

### Q: How do I delete a ruleset?
**A:** Click the three-dot menu → Delete ruleset.

---

## Quick Checklist

After creating all 3 rulesets:

- [ ] Created `master-protection` ruleset
  - [ ] Target: `master`
  - [ ] PR required: 1 approval
  - [ ] Conversation resolution required
  - [ ] Force push prevented
  - [ ] Deletion prevented

- [ ] Created `develop-protection` ruleset
  - [ ] Target: `develop`
  - [ ] PR required: 1 approval
  - [ ] Status checks: `build_check`, `test`
  - [ ] Up to date required
  - [ ] Conversation resolution required
  - [ ] Force push prevented
  - [ ] Deletion prevented

- [ ] Created `release-protection` ruleset
  - [ ] Target: `release`
  - [ ] Force push prevented
  - [ ] Deletion prevented
  - [ ] (Optional: PR required)

- [ ] Tested with PR to `develop`
- [ ] Verified CI runs automatically
- [ ] Tested merge workflow

---

## Related Documentation

- `docs/index.md` - Main developer guide
- `docs/WORKFLOW-AND-BRANCH-PROTECTION.md` - Detailed workflow
- `docs/SETUP-COMPLETE-SUMMARY.md` - Overall setup

---

**Status: 🟢 Ready to create rulesets**
