# TaxNest Batch 2 — Smart Records & Tax Review

This release is an **offline-first** refinement of Batch 1 and retains the same `com.taxnest.app` application ID and SQLite schema. The user-entered SQLite data is preserved if Android successfully installs the update over the previous version using a compatible signing key.

## Features

- **Smart receipt category suggestions:** on-device merchant/receipt text rules, editable before save; no assumption of tax deductibility.
- **Possible duplicate warning:** merchant + amount + close dates checked against existing transactions. Users must explicitly confirm if they want to save an apparent duplicate.
- **Smart Inbox:** users may **paste** emailed order confirmations or deliberately share plain text through Android. The app proposes merchant, total, category and date, then shows a review dialog. No Gmail account link or mailbox permissions exist.
- **Recurring payment insights:** two or more similarly sized charges to the same merchant at weekly, monthly or yearly intervals offer a user-approved subscription suggestion.
- **Annual report:** income/expense/category totals for the selected year, receipt evidence counts, overtime premium logs and mileage. Export a PDF through Android's Save dialog. No automatic deduction or refund claims.
- **Tax-break review improvements:** shows local receipt evidence counts and flags common work-related expenses for review; W-2 and 1099 rules continue to be distinguished.
- **CSV restore:** import previous TaxNest CSV backups, skipping matching records, through the Android document picker.

## IMPORTANT: debug APK signing and existing installs

GitHub-hosted builds may generate a different Android **debug signing key** for each runner. If Android reports "App not installed" or signature incompatibility on updating Batch 1, **export CSV first while Batch 1 is still installed**, ensure original receipt images are preserved in Pictures/TaxNest/Receipts, then uninstall Batch 1 and install Batch 2, and choose More > Restore TaxNest CSV. Uninstalling an app deletes its private database; receipt `content://` references from the prior installation may not survive as working gallery links even if the image files remain. Back them up separately. For seamless future upgrades, configure a stable secure signing identity (do not commit a production private signing key to a public repository).

## Not yet implemented

- Full automatic Gmail OAuth and inbox synchronization (requires approved Google scopes, consent, and potentially app verification/security assessment).
- Direct Capital One, Dave or credit-bureau feeds.
- Private/encrypted external photo storage; exported photos are visible to apps with photo permission.
- Tax filing or automated determination of allowable write-offs.
- Automated bank transaction reconciliation.

## Test checklist

1. Batch 1 entries remain visible when updated with the same signing key; otherwise use CSV restore.
2. Scan receipt: original image preserved, suggested merchant/category editable, duplicate confirmation required if same purchase already exists.
3. Smart Inbox: paste text with a From, Order total and ISO date; confirm and save.
4. Share plain text from Android to TaxNest; confirm review and save.
5. Record two monthly charges of the same price; verify one subscription suggestion and accept it.
6. Generate PDF annual summary, open it and compare amounts with entries.
7. Export CSV and import it; confirm duplicates are skipped.
8. Confirm no Gmail or bank credentials are requested.
