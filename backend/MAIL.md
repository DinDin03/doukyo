# Sending verification emails

The sign-up code is delivered by `EmailSender`. `EmailConfig` picks the
implementation at startup and logs which one:

| Startup log | What happens |
|---|---|
| `Email: sending over SMTP as …` | A real message is sent. |
| `Email: NO SMTP configured — …` | Nothing is sent; the code goes to the backend log. |

It switches on two things: Spring Boot only creates a `JavaMailSender` when
`spring.mail.host` is set, and `DOUKYO_MAIL_FROM` must be non-blank. The fallback
exists so the app still starts without mail configured — it is **not** a mode to
develop in, and it must never reach production, because it is a "log every
verification code" feature.

## Setup (Gmail)

Gmail needs an **App Password**, not your account password. App passwords only
exist once 2-Step Verification is on.

1. Turn on 2-Step Verification —
   https://myaccount.google.com/signinoptions/two-step-verification
2. Create an app password — https://myaccount.google.com/apppasswords
   Name it "Doukyo". Google shows 16 characters in four groups; the spaces are
   only for readability.
3. Copy the template. `.env.local` is gitignored; `.env.example` is not:

```bash
cp backend/.env.example backend/.env.local
```

4. Fill in `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` and `DOUKYO_MAIL_FROM`.
5. Run:

```bash
./scripts/run-backend.sh
```

The script exists so credentials come from a file rather than the command line: a
password typed into a shell is kept in history and is visible in `ps` to anyone
else on the machine.

If an app password is ever pasted somewhere it should not be — a chat, a commit,
a screenshot — revoke it at https://myaccount.google.com/apppasswords and issue a
new one. Revoking is instant and affects nothing else on the account.

## Gotchas

**Keep `DOUKYO_MAIL_FROM` on the same address as `SPRING_MAIL_USERNAME`.** Gmail
only lets you send as the authenticated account or a verified alias, and rewrites
or rejects anything else. This is the most common Gmail SMTP mistake.

**Delivery failures never reach the user.** `startSignUp` already answered "if
that address can receive mail, a code is on the way" — surfacing a provider
outage would break that promise and leak that the address was accepted. A failed
send is logged and nothing else, so **watch the backend log**, not the app.

| Symptom | Cause |
|---|---|
| `Username and Password not accepted` | App password wrong, or 2-Step Verification not actually on |
| `Email: NO SMTP configured` at startup | `.env.local` missing, or a value left as a placeholder |
| Mail lands in spam | Usually a From / username mismatch |

## Moving off Gmail later

Gmail rate-limits hard and will flag bulk sending, so it is for testing only.
Every provider (Resend, Postmark, SendGrid) speaks SMTP, so switching is a change
of credentials in `.env.local` — **no code changes**:

```
SPRING_MAIL_HOST=smtp.resend.com
SPRING_MAIL_USERNAME=resend
SPRING_MAIL_PASSWORD=re_your_api_key
DOUKYO_MAIL_FROM=Doukyo <onboarding@resend.dev>
```

## Note on transactions

Sends happen **after the transaction commits**, not inside it. SMTP is a network
call, and holding a pooled database connection across it starves the pool; a
rollback after sending would also email a code for a sign-up that no longer
exists.
