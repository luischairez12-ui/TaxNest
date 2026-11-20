# TaxNest — Batch 3 (UI polish + optional Gmail)

**What's new:** replaced translucent Material Surface cards with consistent opaque lavender/white cards to address bright rectangular render artifacts seen on OnePlus/OxygenOS; added optional, on-demand **read-only Gmail authorization and receipt review** using the Google AuthorizationClient and Gmail API. The connection requires Google Cloud Gmail API + OAuth Android client configuration matching the APK signing SHA-1, and may require verification. No Gmail data is read until the user explicitly authorizes and initiates a sync. Only user-approved receipts are saved to the SQLite database.

See [GMAIL_SETUP.md](GMAIL_SETUP.md) for setup requirements, privacy limitations and signing guidance.

# TaxNest — Android app, Batch 2

**A real native Android / Kotlin / Jetpack Compose project, based on the approved Mi Transfer / Mi Tidy glassmorphism concept.**

This repository contains implementation source, local storage, screens, and a GitHub Actions APK build workflow. **GitHub Actions builds the installable debug APK.** The source was authored and its pure Kotlin receipt parser and overtime math were smoke-tested, but an end-to-end Android build and device test could not be completed in the authoring environment because Android SDK and Gradle were unavailable.

## What works in this first implementation (when built)

- Home: gradient mountain banner, 2026-style tax year selector, quick tiles, summary based on manual entries (not fake account balances).
- Receipt Vault: Android camera capture, photo import, Android share-image intake, **full original photo saved to `Pictures/TaxNest/Receipts/YYYY/MM`**, on-device ML Kit Latin-script OCR, editable review, merchant/amount/date/categories, receipt thumbnails and original-image open.
- Spending: local SQLite ledger for manual income and expenses, categories, monthly spending overview, CSV export.
- Subscriptions: manual entry, billing frequency, due date, monthly equivalent total, deletion.
- Overtime: weekly regular rate and OT hours, provisional FLSA half-time-premium tracking, annual deduction cap labels. Does not file taxes or guarantee eligibility.
- Mileage: manual work-trip logging, work purpose, reimbursement status, W-2 vs. 1099 explanations.
- Tax Break Finder: potential review list personalized by W-2/1099 status, including overtime, reimbursements, possible contractor expenses, education/retirement/HSA/energy tax items. **These are prompts for review, not claims that a deduction applies.**
- Credit Comeback: manual FICO/credit card balance & limit entry, utilization feedback, responsible payoff checklist.
- Personal tax profile: W-2/1099, filing status, dependents, tax year (2025–2028).
- Offline-first: no paid server, no Gmail token, no bank login, no ads.

## Batch 2 additions

See [BATCH2_NOTES.md](BATCH2_NOTES.md). New: Smart Inbox for user-shared email text (not OAuth), receipt duplicate review, category suggestions, recurring subscription hints, year-end PDF report and CSV restore. All monetary records remain on-device.

### Not yet implemented

- Gmail OAuth login / automatic receipt and subscription imports. The concept screen is explicitly marked **not connected**, because Gmail access requires user consent, integration, and possibly Google restricted-scope verification.
- Bank account sync or transaction feeds; cards only show records you personally enter.
- Full encryption of exported/public receipt photos; these images intentionally appear in the shared Android Pictures folder and may be accessible to other photo-capable apps. The SQLite ledger is stored privately by Android, **but it is not encrypted with SQLCipher**.
- PDF W-2/1099 tax document vault, encrypted backup, advanced receipt deduplication, tax-filing service, credit-bureau lookup, automatic mileage/GPS tracking, BoreLab Field sync.
- Pixel-perfect parity with the inspiration image. Batch 1 establishes its layout, visual language, and actual screens; device testing and fine-tuning remain.

## Build an APK using GitHub Actions (without an Android PC)

1. Create a new GitHub repository named **TaxNest** (do **not** upload these files into an existing BoreLab Field or Thor launcher repository).
2. Upload the **contents** of this source folder into the repository root, including `.github/workflows/build-apk.yml`.
3. From your repository's **Actions** tab, open **Build TaxNest APK**, then choose **Run workflow**. Pushing to `main` or `master` also triggers a build.
4. When the run succeeds, open its **TaxNest-debug-apk** artifact, download and unzip it. Install `app-debug.apk` on your Android phone, approving the browser/file manager's Install Unknown Apps prompt when required.
5. If the run fails, read the Actions job logs. Debug APKs are for personal testing, not release-store distribution.

The workflow uses JDK 17, Gradle 8.13, Android SDK 36, Kotlin 2.2.20, and Compose BOM 2025.08.00.

## Build locally

- Open the `TaxNest` directory in Android Studio with Android SDK 36 installed and JDK 17 selected.
- If Android Studio prompts to generate the Gradle wrapper, choose Gradle 8.13; or run `gradle wrapper --gradle-version 8.13` with an existing Gradle installation. A wrapper JAR is not bundled in this archive.
- Run `gradle :app:assembleDebug` after Gradle is configured. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Security / tax notes

- **Never paste online banking or Google account passwords into TaxNest.** No account connections exist in Batch 1.
- Images are visible under `Pictures/TaxNest/Receipts`. Other apps granted photo access may read them. Do not store Social Security numbers, full credit card numbers, or confidential documents in these photos until encryption is added.
- If you delete a ledger entry, its preserved receipt image remains in Pictures until you delete that image through the Android gallery/file manager.
- Exported CSV is unencrypted and references `content://` photo URIs; the originals must be separately copied to back them up to a different device.
- For W-2 employees, routine unreimbursed mileage generally is not an individual federal employee business deduction. TaxNest distinguishes tracking for company reimbursement from potential 1099 business expenses.
- For 2026–2028 qualified overtime, confirm the FLSA portion reported by payroll, generally Form W-2 box 12 code TT, and applicable income phaseouts, limits and filing rules. Calculations are **estimates, not a tax return**.

## Manual QA checklist

- [ ] Install app and see Home, Receipts, Money, Tax Breaks, More.
- [ ] Scan a paper receipt; confirm the high-resolution image exists in Android `Pictures/TaxNest/Receipts` and text can be corrected.
- [ ] Import a photo; confirm saved image and expense after review.
- [ ] Share a photo from another Android app to TaxNest.
- [ ] Enter income, expenses and categories; quit and relaunch; verify persistence.
- [ ] Add/edit profile and subscriptions; relaunch; verify persistence.
- [ ] Enter one week with $38 regular rate, 30 FLSA overtime hours; verify estimated premium of $570, not $1,710.
- [ ] Add a truck mileage trip; change W-2/1099 mode; see different guidance.
- [ ] Enter credit snapshot; confirm credit utilization.
- [ ] Export CSV; confirm expenses, overtime, subscriptions and mileage lines.
- [ ] Verify app does not ask for Gmail or banking password and never claims Gmail is connected.

## Official rules / development references

- IRS overtime rules: https://www.irs.gov/newsroom/treasury-irs-issue-faqs-to-address-the-new-deduction-for-qualified-overtime-compensation-under-the-one-big-beautiful-bill
- Android shared media files: https://developer.android.com/training/data-storage/shared/media
- ML Kit on-device text recognition: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
- Compose BOM: https://developer.android.com/develop/ui/compose/bom
- Google Play API levels: https://developer.android.com/google/play/requirements/target-sdk

## Design reference

See `design/approved_taxnest_concept.png`. The first icy blue/lavender layout is the approved visual direction. Use that reference for the next pixel-polish batch.
