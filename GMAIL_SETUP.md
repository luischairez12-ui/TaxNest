# TaxNest Android Gmail authorization setup (developer)

Batch 3 contains an on-device OAuth authorization request and on-demand Gmail REST receipt scan. It **will not work until Google Cloud is configured for the exact APK signing certificate**. Do not enter Google credentials into TaxNest. There are no hard-coded secrets and no always-on background access.

1. Create a Google Cloud project, enable **Gmail API** under APIs & Services.
2. Configure the OAuth consent screen/brand (TaxNest, support contact, privacy policy, terms). For a personal testing app, register your own Google address as a test user if required by Google.
3. Create an **Android OAuth client**, package `com.taxnest.app`, with the **SHA-1 of the certificate signing the installed APK**. Be aware that GitHub Actions generated debug keys may change between runs. Before relying on Gmail, switch to a stable signing key handled through GitHub Actions secrets, not a key committed to this public repository.
4. Declare the Gmail read-only scope: `https://www.googleapis.com/auth/gmail.readonly` (restricted). Follow Google's verification policies for distribution. Gmail API permission grants broad mailbox reading even though TaxNest's code only requests likely receipts. There may be verification or assessment requirements.
5. Build and install the matching signed APK. Go to **More / Smart Inbox > Connect Gmail (read-only)**. Confirm Google's consent screen. Tap Sync to fetch possible purchase emails. Tap Review to manually save any purchase.
6. The app does not store the access token or raw Gmail messages across sessions. No auto-import and no background scan. To revoke, use Disconnect in the app or revoke third-party access in Google Account settings.

## Additional safeguards
- Google access tokens must never be logged, published, or committed to GitHub.
- The GitHub repository was public at the time Batch 3 was authored; never commit receipts, financial account details, OAuth secrets, or a production signing key.
- Keep the Android Gmail scope read-only, and ask only when the user opens Smart Inbox.
- Gmail syncing is on-demand, limited to the 30 newest search results from 18 months. Unselected email contents stay in memory and are forgotten when the app closes.
- Gmail results can be incomplete or contain unrelated promotional messages. Every candidate must be reviewed before saving a transaction.
- If you need automatic background syncing in a later release, build a separate consent, secure backend, and verification process first.

Docs: https://developer.android.com/identity/authorization and https://developers.google.com/workspace/gmail/api/auth/scopes
